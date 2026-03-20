<script setup lang="ts">
import { ref, nextTick, onUnmounted } from 'vue'
import gsap from 'gsap'
import Hls from 'hls.js'
import { STATION_LIST } from '@/utils/service'
import type { PlayerLogLine } from '@/types'

// ── State ─────────────────────────────────────────────────────────────────────
const playerStation     = ref('lumisonic')
const playerStatus      = ref('idle')
const playerFragCount   = ref(0)
const playerErrorCount  = ref(0)
const playerLastFragSize = ref('—')
const playerLogs        = ref<PlayerLogLine[]>([])
const playerNpTitle     = ref('')
const playerNpArtist    = ref('')
const isPlaying         = ref(false)
const playerVolume      = ref(1)
const isWaveformActive  = ref(false)
const playerStreamLabel = ref('—')

// Template refs
const waveformCanvas = ref<HTMLCanvasElement | null>(null)
const playerAudio    = ref<HTMLAudioElement | null>(null)
const playerLogEl    = ref<HTMLElement | null>(null)

// Internal HLS + Web Audio state (not reactive)
let pHls: Hls | null = null
let pHlsDestroyed = true
let pAudioEventsAttached = false
let pAudioCtx: AudioContext | null = null
let pAnalyser: AnalyserNode | null = null
let pSource: MediaElementAudioSourceNode | null = null
let pAnimId: number | null = null

// ── Logging ───────────────────────────────────────────────────────────────────
function playerLog(msg: string, type: PlayerLogLine['type'] = 'info') {
  const now = new Date()
  const ts = now.toTimeString().slice(0, 8) + '.' + String(now.getMilliseconds()).padStart(3, '0')
  playerLogs.value.push({ ts, msg, type })
  if (playerLogs.value.length > 200) playerLogs.value.shift()
  nextTick(() => { if (playerLogEl.value) playerLogEl.value.scrollTop = playerLogEl.value.scrollHeight })
}

function setPlayerStatus(label: string, cls: string) {
  playerStatus.value = cls === 'playing' ? 'playing' : label
}

// ── Waveform ──────────────────────────────────────────────────────────────────
function drawWaveform() {
  const canvas = waveformCanvas.value
  if (!canvas || !pAnalyser) return
  const ctx = canvas.getContext('2d')!
  const W = canvas.offsetWidth, H = canvas.offsetHeight
  canvas.width = W; canvas.height = H
  const buf = new Uint8Array(pAnalyser.frequencyBinCount)
  function frame() {
    pAnimId = requestAnimationFrame(frame)
    pAnalyser!.getByteFrequencyData(buf)
    ctx.clearRect(0, 0, W, H)
    const bw = W / buf.length
    buf.forEach((v, i) => {
      const h = (v / 255) * H, alpha = 0.3 + (v / 255) * 0.7
      ctx.fillStyle = `rgba(33,150,243,${alpha})`
      ctx.fillRect(i * bw, H - h, bw - 1, h)
    })
  }
  frame()
}

function startWaveformViz() {
  if (pAudioCtx || !playerAudio.value) return
  try {
    pAudioCtx = new (window.AudioContext || (window as any).webkitAudioContext)()
    pAnalyser = pAudioCtx.createAnalyser()
    pAnalyser.fftSize = 128
    pSource = pAudioCtx.createMediaElementSource(playerAudio.value)
    pSource.connect(pAnalyser)
    pAnalyser.connect(pAudioCtx.destination)
    isWaveformActive.value = true
    drawWaveform()
  } catch (e: any) { playerLog('waveform unavailable: ' + e.message, 'warn') }
}

function stopWaveformViz() {
  if (pAnimId) { cancelAnimationFrame(pAnimId); pAnimId = null }
  isWaveformActive.value = false
  if (waveformCanvas.value) {
    const ctx = waveformCanvas.value.getContext('2d')
    ctx?.clearRect(0, 0, waveformCanvas.value.width, waveformCanvas.value.height)
  }
}

// ── Audio events ──────────────────────────────────────────────────────────────
function bindPlayerAudioEvents() {
  if (pAudioEventsAttached || !playerAudio.value) return
  pAudioEventsAttached = true
  const a = playerAudio.value
  a.addEventListener('play',    () => { isPlaying.value = true })
  a.addEventListener('pause',   () => { isPlaying.value = false })
  a.addEventListener('playing', () => { setPlayerStatus('playing', 'playing'); startWaveformViz() })
  a.addEventListener('waiting', () => setPlayerStatus('buffering…', 'buffering'))
  a.addEventListener('stalled', () => setPlayerStatus('stalled…', 'buffering'))
  a.addEventListener('ended',   () => { setPlayerStatus('ended', 'stopped'); stopWaveformViz() })
  a.addEventListener('error',   () => {
    playerLog('audio error: ' + (a.error ? a.error.code : '?'), 'error')
    setPlayerStatus('error', 'error')
    playerErrorCount.value++
  })
  ;['play', 'pause', 'playing', 'waiting', 'stalled', 'ended'].forEach(ev =>
    a.addEventListener(ev, () => playerLog('audio: ' + ev, 'event'))
  )
}

// ── Now playing ───────────────────────────────────────────────────────────────
function playerUpdateNowPlaying(frag: any) {
  const title = frag.title || ''; if (!title) return
  const di = title.indexOf(' - ')
  const t  = di !== -1 ? title.slice(0, di).trim() : title.trim()
  const ar = di !== -1 ? title.slice(di + 3).trim() : ''
  if (t === playerNpTitle.value && ar === playerNpArtist.value) return
  gsap.to({ v: 0 }, { v: 1, duration: 0.15, onComplete() {
    playerNpTitle.value  = t  || '—'
    playerNpArtist.value = ar || '—'
  }})
  playerLog('now playing: ' + title, 'ok')
}

// ── Stop / Load / Play ────────────────────────────────────────────────────────
function playerStop() {
  pHlsDestroyed = true
  if (pHls) { pHls.destroy(); pHls = null; playerLog('hls destroyed', 'event') }
  if (playerAudio.value) {
    playerAudio.value.pause()
    playerAudio.value.removeAttribute('src')
    playerAudio.value.load()
  }
  pAudioEventsAttached = false
  stopWaveformViz()
  setPlayerStatus('stopped', 'stopped')
  isPlaying.value = false
  playerStreamLabel.value = '—'
  playerLog('stopped.', 'info')
}

function playerLoad(src: string) {
  playerStop(); pHlsDestroyed = false
  playerLog('─── load: ' + src, 'info')
  setPlayerStatus('loading…', 'buffering')
  playerStreamLabel.value = src.split('/').pop() ?? src
  const a = playerAudio.value!
  const isSafari = /^((?!chrome|android).)*safari/i.test(navigator.userAgent)
  if (isSafari && a.canPlayType('application/vnd.apple.mpegurl')) {
    playerLog('native HLS (Safari)', 'ok'); a.src = src
    bindPlayerAudioEvents(); a.play().catch(e => playerLog('play() rejected: ' + e.message, 'warn')); return
  }
  if (!Hls.isSupported()) { playerLog('hls.js not supported', 'error'); return }
  playerLog('hls.js v' + Hls.version, 'info')
  pHls = new Hls({ enableWorker: true, debug: false, startPosition: -1, liveSyncDurationCount: 3, liveMaxLatencyDurationCount: 5 })
  pHls.on(Hls.Events.MANIFEST_PARSED, (_e, d) => {
    playerLog(`manifest parsed — ${d.levels.length} level(s)`, 'ok')
    a.play().catch(e => playerLog('play() rejected: ' + e.message, 'warn'))
  })
  pHls.on(Hls.Events.FRAG_LOADED, (_e, d) => {
    playerFragCount.value++
    const loaded = (d as any).stats?.loaded ?? (d as any).frag?.stats?.loaded ?? 0
    const kb = (loaded / 1024).toFixed(1)
    playerLog(`frag [sn=${d.frag.sn}] ${kb} KB`, 'ok')
    playerLastFragSize.value = kb + ' KB'
    playerUpdateNowPlaying(d.frag)
  })
  pHls.on(Hls.Events.ERROR, (_e, data) => {
    if (pHlsDestroyed) return
    const fatal = data.fatal ? ' [FATAL]' : ''
    playerLog(`${data.type}/${data.details}${fatal}`, data.fatal ? 'error' : 'warn')
    if (!data.fatal) return
    playerErrorCount.value++
    if (data.type === Hls.ErrorTypes.NETWORK_ERROR) pHls!.startLoad()
    else if (data.type === Hls.ErrorTypes.MEDIA_ERROR) pHls!.recoverMediaError()
    else { pHls!.destroy(); pHls = null }
  })
  pHls.loadSource(src); pHls.attachMedia(a); bindPlayerAudioEvents()
  playerLog('hls configured', 'info')
}

function togglePlay() {
  const a = playerAudio.value; if (!a) return
  if (!a.src && !pHls) { playerLoad(`/stream/${playerStation.value}/stream.m3u8`); return }
  if (a.paused) a.play().catch(e => playerLog('play() rejected: ' + e.message, 'warn'))
  else a.pause()
}

function updateVolume() { if (playerAudio.value) playerAudio.value.volume = Number(playerVolume.value) }

function playerOnStationChange() {
  if (pHls && !pHlsDestroyed) playerLoad(`/stream/${playerStation.value}/stream.m3u8`)
}

async function playerServerAction(method: 'POST' | 'DELETE') {
  const brand = playerStation.value
  playerLog(`${method === 'POST' ? 'starting' : 'stopping'} server stream [${brand}]`, 'info')
  try {
    const r = await fetch('/aivox/' + brand + '/command/', { method })
    const text = await r.text()
    playerLog(`server ${r.status}: ${text}`, r.ok ? 'ok' : 'error')
    if (r.ok) gsap.fromTo('.player-card', { borderColor: 'rgba(33,150,243,0.8)' }, { borderColor: 'rgba(51,51,51,1)', duration: 1.2 })
  } catch (e: any) { playerLog('server action failed: ' + e.message, 'error') }
}

function copyPlayerLog() {
  const text = playerLogs.value.map(l => `${l.ts} ${l.msg}`).join('\n')
  navigator.clipboard.writeText(text).then(() => playerLog('log copied', 'ok'))
}

onUnmounted(() => { playerStop() })
</script>

<template>
  <!-- Sidebar content -->
  <Teleport to="#view-sidebar">
    <div class="sidebar-section">
      <span class="section-label">Station</span>
      <div class="select-wrap">
        <select class="station-select" v-model="playerStation" @change="playerOnStationChange">
          <option v-for="s in STATION_LIST" :key="s" :value="s">{{ s }}</option>
        </select>
      </div>
    </div>
    <div class="sidebar-divider"></div>
    <div class="sidebar-section">
      <div class="stat-item">
        <span class="stat-label">frags loaded</span>
        <span class="stat-value">{{ playerFragCount }}</span>
      </div>
      <div class="stat-item">
        <span class="stat-label">last frag</span>
        <span class="stat-value amber small">{{ playerLastFragSize }}</span>
      </div>
      <div class="stat-item">
        <span class="stat-label">errors</span>
        <span class="stat-value red">{{ playerErrorCount }}</span>
      </div>
    </div>
    <div class="status-indicator" style="margin-top:0; border-top: 1px solid var(--border);">
      <div class="status-row">
        <div class="status-dot" :class="playerStatus"></div>
        <span class="status-text">{{ playerStatus }}</span>
      </div>
    </div>
  </Teleport>

  <!-- Topbar action slot -->
  <Teleport to="#topbar-action">
    <div class="live-badge" v-show="playerStatus === 'playing'">
      <div class="live-dot"></div><span>LIVE</span>
    </div>
  </Teleport>

  <!-- Main content -->
  <main class="player-main">
    <div class="player-card">
      <div class="player-card-body">
        <div class="now-playing" v-show="playerNpTitle">
          <div class="np-label">now playing</div>
          <div class="np-title">{{ playerNpTitle }}</div>
          <div class="np-artist">{{ playerNpArtist }}</div>
        </div>
        <div class="waveform-wrap">
          <canvas ref="waveformCanvas" class="waveform-canvas"></canvas>
          <div class="waveform-idle" v-show="!isWaveformActive">no signal</div>
        </div>
        <div class="player-controls">
          <button class="play-btn" :class="{ active: isPlaying }" @click="togglePlay">
            {{ isPlaying ? '❚❚' : '▶' }}
          </button>
          <span class="stream-label">{{ playerStreamLabel }}</span>
          <div class="volume-wrap">
            <span class="vol-label">vol</span>
            <input type="range" v-model="playerVolume" min="0" max="1" step="0.02" @input="updateVolume">
          </div>
        </div>
        <div class="sidebar-divider" style="margin: 4px 0;"></div>
        <div style="display:flex; align-items:center; justify-content:space-between; flex-wrap:wrap; gap:8px;">
          <span class="card-label">aivox control</span>
          <div class="btn-row">
            <button class="action-btn" @click="playerServerAction('POST')">start stream</button>
            <button class="action-btn danger" @click="playerServerAction('DELETE')">stop stream</button>
          </div>
        </div>
      </div>
    </div>
    <div class="log-card">
      <div class="log-card-header">
        <span class="card-label">log</span>
        <button class="action-btn" @click="copyPlayerLog">copy</button>
      </div>
      <div class="log-body" ref="playerLogEl">
        <div v-for="(line, i) in playerLogs" :key="i" class="log-line">
          <span class="log-ts">{{ line.ts }}</span>
          <span :class="'log-' + line.type">{{ line.msg }}</span>
        </div>
      </div>
    </div>
    <audio ref="playerAudio" style="display:none"></audio>
  </main>
</template>
