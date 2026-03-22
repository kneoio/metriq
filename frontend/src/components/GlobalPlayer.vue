<script setup lang="ts">
/**
 * GlobalPlayer — always mounted in App.vue (never destroyed on navigation).
 * Owns the <audio> element and all HLS.js / Web Audio logic.
 * All state is written to the aivox store so any view can read it.
 */
import { ref, watch, onMounted, onUnmounted } from 'vue'
import Hls from 'hls.js'
import { useAivoxStore } from '@/stores/aivox'

const aivox = useAivoxStore()

const audioEl = ref<HTMLAudioElement | null>(null)

let pHls: Hls | null              = null
let pHlsDestroyed                 = true
let pAudioEventsAttached          = false
let pAudioCtx: AudioContext | null = null
let pSource: MediaElementAudioSourceNode | null = null

// ── Waveform ────────────────────────────────────────────────────────────────
function startWaveformViz() {
  if (pAudioCtx || !audioEl.value) return
  try {
    pAudioCtx        = new (window.AudioContext || (window as any).webkitAudioContext)()
    const node       = pAudioCtx.createAnalyser()
    node.fftSize     = 128
    pSource          = pAudioCtx.createMediaElementSource(audioEl.value)
    pSource.connect(node)
    node.connect(pAudioCtx.destination)
    aivox.analyser           = node
    aivox.isWaveformActive   = true
  } catch (e: any) {
    aivox.log('waveform unavailable: ' + e.message, 'warn')
  }
}

function stopWaveformViz() {
  aivox.analyser         = null
  aivox.isWaveformActive = false
}

// ── Audio events ─────────────────────────────────────────────────────────────
function bindAudioEvents() {
  if (pAudioEventsAttached || !audioEl.value) return
  pAudioEventsAttached = true
  const a = audioEl.value
  a.addEventListener('play',    () => { aivox.isPlaying = true })
  a.addEventListener('pause',   () => { aivox.isPlaying = false })
  a.addEventListener('playing', () => { aivox.status = 'playing'; startWaveformViz() })
  a.addEventListener('waiting', () => { aivox.status = 'buffering' })
  a.addEventListener('stalled', () => { aivox.status = 'buffering' })
  a.addEventListener('ended',   () => { aivox.status = 'stopped';  stopWaveformViz() })
  a.addEventListener('error',   () => {
    aivox.log('audio error: ' + (a.error ? a.error.code : '?'), 'error')
    aivox.status = 'error'; aivox.errorCount++
  })
  ;['play', 'pause', 'playing', 'waiting', 'stalled', 'ended'].forEach(ev =>
    a.addEventListener(ev, () => aivox.log('audio: ' + ev, 'event'))
  )
}

// ── Now playing ──────────────────────────────────────────────────────────────
function setNowPlaying(t: string, ar: string) {
  if (t === aivox.npTitle && ar === aivox.npArtist) return
  aivox.npTitle  = t  || '—'
  aivox.npArtist = ar || '—'
  aivox.log('now playing: ' + [t, ar].filter(Boolean).join(' — '), 'ok')
}

function updateNowPlayingFromExtInf(frag: any) {
  const title = frag.title || ''; if (!title) return
  const di = title.indexOf(' - ')
  setNowPlaying(
    di !== -1 ? title.slice(0, di).trim() : title.trim(),
    di !== -1 ? title.slice(di + 3).trim() : ''
  )
}

function updateNowPlayingFromID3(data: Uint8Array) {
  try {
    const txt = new TextDecoder('latin1').decode(data)
    const getTag = (tag: string) => {
      const idx = txt.indexOf(tag); if (idx === -1) return ''
      // skip 10-byte frame header, then skip encoding byte
      const start = idx + 10 + 1
      const end   = txt.indexOf('\0', start + 1)
      return txt.slice(start, end > start ? end : start + 100)
        .replace(/[^\x20-\x7E\u00C0-\u024F]/g, '').trim()
    }
    const t  = getTag('TIT2')
    const ar = getTag('TPE1')
    if (t || ar) setNowPlaying(t, ar)
  } catch { /* ignore malformed ID3 */ }
}

// ── Stop / Load ───────────────────────────────────────────────────────────────
function playerStop() {
  pHlsDestroyed = true
  if (pHls) { pHls.destroy(); pHls = null; aivox.log('hls destroyed', 'event') }
  if (audioEl.value) {
    audioEl.value.pause()
    audioEl.value.removeAttribute('src')
    audioEl.value.load()
  }
  pAudioEventsAttached = false
  stopWaveformViz()
  aivox.status            = 'stopped'
  aivox.isPlaying         = false
  aivox.playerStreamLabel = '—'
  aivox.log('stopped.', 'info')
}

function playerLoad(src: string) {
  playerStop(); pHlsDestroyed = false
  aivox.log('─── load: ' + src, 'info')
  aivox.status            = 'buffering'
  aivox.playerStreamLabel = src.split('/').pop() ?? src
  const a = audioEl.value!
  const isSafari = /^((?!chrome|android).)*safari/i.test(navigator.userAgent)
  if (isSafari && a.canPlayType('application/vnd.apple.mpegurl')) {
    aivox.log('native HLS (Safari)', 'ok')
    a.src = src; bindAudioEvents()
    a.play().catch(e => aivox.log('play() rejected: ' + e.message, 'warn'))
    return
  }
  if (!Hls.isSupported()) { aivox.log('hls.js not supported', 'error'); return }
  aivox.log('hls.js v' + Hls.version, 'info')
  pHls = new Hls({ enableWorker: true, debug: false, startPosition: -1, liveSyncDurationCount: 3, liveMaxLatencyDurationCount: 5 })
  pHls.on(Hls.Events.MANIFEST_PARSED, (_e, d) => {
    aivox.log(`manifest parsed — ${d.levels.length} level(s)`, 'ok')
    a.play().catch(e => aivox.log('play() rejected: ' + e.message, 'warn'))
  })
  pHls.on(Hls.Events.FRAG_LOADED, (_e, d) => {
    aivox.fragCount++
    const kb = ((((d as any).stats?.loaded ?? 0) / 1024)).toFixed(1)
    aivox.log(`frag [sn=${d.frag.sn}] ${kb} KB`, 'ok')
    aivox.lastFragSize = kb + ' KB'
    updateNowPlayingFromExtInf(d.frag)
  })
  pHls.on(Hls.Events.FRAG_PARSING_METADATA, (_e, d) => {
    d.samples?.forEach((s: any) => { if (s.data) updateNowPlayingFromID3(s.data) })
  })
  pHls.on(Hls.Events.ERROR, (_e, data) => {
    if (pHlsDestroyed) return
    const fatal = data.fatal ? ' [FATAL]' : ''
    aivox.log(`${data.type}/${data.details}${fatal}`, data.fatal ? 'error' : 'warn')
    if (!data.fatal) return
    aivox.errorCount++
    if (data.type === Hls.ErrorTypes.NETWORK_ERROR)  pHls!.startLoad()
    else if (data.type === Hls.ErrorTypes.MEDIA_ERROR) pHls!.recoverMediaError()
    else { pHls!.destroy(); pHls = null }
  })
  pHls.loadSource(src); pHls.attachMedia(a); bindAudioEvents()
  aivox.log('hls configured', 'info')
}

// ── Controls ──────────────────────────────────────────────────────────────────
function togglePlay() {
  const a = audioEl.value; if (!a) return
  if (!a.src && !pHls) { playerLoad(`/stream/${aivox.station}/stream.m3u8`); return }
  if (a.paused) a.play().catch(e => aivox.log('play() rejected: ' + e.message, 'warn'))
  else a.pause()
}

// Volume sync
watch(() => aivox.playerVolume, v => { if (audioEl.value) audioEl.value.volume = Number(v) })

// Station change → reload if already playing
watch(() => aivox.station, () => {
  if (pHls && !pHlsDestroyed) playerLoad(`/stream/${aivox.station}/stream.m3u8`)
})

onMounted(() => {
  // Register controls so the store (and topbar play button) can call them
  aivox.registerPlayer({ toggle: togglePlay, load: playerLoad, stop: playerStop })
})

onUnmounted(() => { playerStop() })
</script>

<template>
  <!-- Invisible — just keeps the audio element alive globally -->
  <audio ref="audioEl" style="display:none"></audio>
</template>
