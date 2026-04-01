import { ref, shallowRef, reactive, computed } from 'vue'
import { defineStore } from 'pinia'
import { useContextStore } from '@/stores/context'
import type { PlayerLogLine } from '@/types'

export const useAivoxStore = defineStore('aivox', () => {

  const context = useContextStore()

  // ── Stream state ─────────────────────────────────────────────────────────────
  const fragCount       = ref(0)
  const lastFragSize    = ref('—')
  const errorCount      = ref(0)
  const status          = ref('idle')
  const cmdStatus       = ref('')
  const heartbeatByBrand = reactive<Record<string, boolean | null>>({})

  const heartbeat = computed(() => {
    const b = context.activeBrand
    return b in heartbeatByBrand ? heartbeatByBrand[b] : null
  })

  async function pollHeartbeat() {
    const brand = context.activeBrand
    try {
      const res = await fetch(`/aivox/${brand}/heartbeat`)
      if (!res.ok) { heartbeatByBrand[brand] = false; return }
      heartbeatByBrand[brand] = (await res.text()).trim().toLowerCase() === 'true'
    } catch {
      heartbeatByBrand[brand] = false
    }
  }

  pollHeartbeat()
  setInterval(pollHeartbeat, 30_000)

  async function serverAction(method: 'POST' | 'DELETE') {
    const cmd = method === 'POST' ? 'start' : 'stop'
    cmdStatus.value = method === 'POST' ? 'starting…' : 'stopping…'
    try {
      const r    = await fetch('/aivox/' + context.activeBrand + '/' + cmd, { method })
      const text = await r.text()
      cmdStatus.value = r.ok ? (method === 'POST' ? 'started' : 'stopped') : 'error: ' + text
      if (r.ok && method === 'DELETE') { stopStream(); resetPlayer() }  // stop + reset local player when server stops
    } catch (e: any) {
      cmdStatus.value = 'error: ' + e.message
    }
  }

  // ── Player state (global — persists across navigation) ────────────────────────
  const isPlaying         = ref(false)
  const npTitle           = ref('')
  const npArtist          = ref('')
  const playerStreamLabel = ref('—')
  const playerLogs        = ref<PlayerLogLine[]>([])
  const isWaveformActive  = ref(false)
  const playerVolume      = ref(1)
  // shallowRef so Vue doesn't try to deep-observe the AnalyserNode DOM object
  const analyser          = shallowRef<AnalyserNode | null>(null)

  // Callbacks registered by GlobalPlayer once it mounts
  let _toggle: (() => void) | null            = null
  let _load:   ((src: string) => void) | null = null
  let _stop:   (() => void) | null            = null
  let _reset:  (() => void) | null            = null

  function registerPlayer(fns: { toggle: () => void; load: (src: string) => void; stop: () => void; reset: () => void }) {
    _toggle = fns.toggle
    _load   = fns.load
    _stop   = fns.stop
    _reset  = fns.reset
  }

  function togglePlay()            { _toggle?.() }
  function loadStream(src: string) { _load?.(src) }
  function stopStream()            { _stop?.() }
  function resetPlayer()           { _reset?.() }

  function log(msg: string, type: PlayerLogLine['type'] = 'info') {
    const now = new Date()
    const ts  = now.toTimeString().slice(0, 8) + '.' + String(now.getMilliseconds()).padStart(3, '0')
    playerLogs.value.push({ ts, msg, type })
    if (playerLogs.value.length > 200) playerLogs.value.shift()
  }

  return {
    // stream
    fragCount, lastFragSize, errorCount, status, cmdStatus, heartbeat, serverAction,
    // player
    isPlaying, npTitle, npArtist, playerStreamLabel, playerLogs,
    isWaveformActive, playerVolume, analyser,
    registerPlayer, togglePlay, loadStream, stopStream, resetPlayer, log,
  }
})
