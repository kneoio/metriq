<script setup lang="ts">
import { ref, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { useAivoxStore } from '@/stores/aivox'

const aivox = useAivoxStore()

const waveformCanvas = ref<HTMLCanvasElement | null>(null)
const playerLogEl    = ref<HTMLElement | null>(null)
let   pAnimId: number | null = null

// ── Waveform drawing (reads analyser from store) ───────────────────────────────
function drawWaveform() {
  const canvas   = waveformCanvas.value
  const analyser = aivox.analyser
  if (!canvas || !analyser) return
  const ctx = canvas.getContext('2d')!
  const W = canvas.offsetWidth, H = canvas.offsetHeight
  canvas.width = W; canvas.height = H
  const node = analyser                        // narrowed: definitely not null
  const buf = new Uint8Array(node.frequencyBinCount)
  function frame() {
    pAnimId = requestAnimationFrame(frame)
    node.getByteFrequencyData(buf)
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

function stopDraw() {
  if (pAnimId) { cancelAnimationFrame(pAnimId); pAnimId = null }
  const c = waveformCanvas.value
  if (c) c.getContext('2d')?.clearRect(0, 0, c.width, c.height)
}

// Start drawing when analyser becomes available (or page is re-entered while playing)
watch(() => aivox.analyser, analyser => {
  stopDraw()
  if (analyser) nextTick(() => drawWaveform())
})

// Auto-scroll log
watch(() => aivox.playerLogs.length, () => {
  nextTick(() => { if (playerLogEl.value) playerLogEl.value.scrollTop = playerLogEl.value.scrollHeight })
})

function copyPlayerLog() {
  const text = aivox.playerLogs.map(l => `${l.ts} ${l.msg}`).join('\n')
  navigator.clipboard.writeText(text).then(() => aivox.log('log copied', 'ok'))
}

onMounted(() => {
  // Resume drawing if audio is already playing when user navigates here
  if (aivox.analyser) drawWaveform()
})

onUnmounted(() => { stopDraw() })
</script>

<template>
  <main class="player-main">
    <div class="player-card">
      <div class="player-card-body">

        <div class="now-playing" v-show="aivox.npTitle">
          <div class="np-label">now playing</div>
          <div class="np-title">{{ aivox.npTitle }}</div>
          <div class="np-artist">{{ aivox.npArtist }}</div>
        </div>

        <div class="waveform-wrap">
          <canvas ref="waveformCanvas" class="waveform-canvas"></canvas>
          <div class="waveform-idle" v-show="!aivox.isWaveformActive">no signal</div>
        </div>

        <div class="player-controls">
          <button class="play-btn" :class="{ active: aivox.isPlaying }" @click="aivox.togglePlay()">
            {{ aivox.isPlaying ? '❚❚' : '▶' }}
          </button>
          <span class="stream-label">{{ aivox.playerStreamLabel }}</span>
          <div class="volume-wrap">
            <span class="vol-label">vol</span>
            <input type="range" v-model="aivox.playerVolume" min="0" max="1" step="0.02">
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
        <div v-for="(line, i) in aivox.playerLogs" :key="i" class="log-line">
          <span class="log-ts">{{ line.ts }}</span>
          <span :class="'log-' + line.type">{{ line.msg }}</span>
        </div>
      </div>
    </div>

  </main>
</template>
