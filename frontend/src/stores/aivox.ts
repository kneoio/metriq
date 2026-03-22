import { ref, shallowRef } from 'vue'
import { defineStore } from 'pinia'
import { useContextStore } from '@/stores/context'
import type { PlayerLogLine } from '@/types'

export const useAivoxStore = defineStore('aivox', () => {

  const context = useContextStore()

  // ── Stream state ─────────────────────────────────────────────────────────────
  const fragCount    = ref(0)
  const lastFragSize = ref('—')
  const errorCount   = ref(0)
  const status       = ref('idle')
  const cmdStatus    = ref('')

  async function serverAction(method: 'POST' | 'DELETE') {
    cmdStatus.value = method === 'POST' ? 'starting…' : 'stopping…'
    try {
      const r    = await fetch('/aivox/' + context.activeBrand + '/command/', { method })
      const text = await r.text()
      cmdStatus.value = r.ok ? (method === 'POST' ? 'started' : 'stopped') : 'error: ' + text
      if (r.ok && method === 'DELETE') stopStream()   // stop local player when server stops
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

  function registerPlayer(fns: { toggle: () => void; load: (src: string) => void; stop: () => void }) {
    _toggle = fns.toggle
    _load   = fns.load
    _stop   = fns.stop
  }

  function togglePlay()            { _toggle?.() }
  function loadStream(src: string) { _load?.(src) }
  function stopStream()            { _stop?.() }

  function log(msg: string, type: PlayerLogLine['type'] = 'info') {
    const now = new Date()
    const ts  = now.toTimeString().slice(0, 8) + '.' + String(now.getMilliseconds()).padStart(3, '0')
    playerLogs.value.push({ ts, msg, type })
    if (playerLogs.value.length > 200) playerLogs.value.shift()
  }

  return {
    // stream
    fragCount, lastFragSize, errorCount, status, cmdStatus, serverAction,
    // player
    isPlaying, npTitle, npArtist, playerStreamLabel, playerLogs,
    isWaveformActive, playerVolume, analyser,
    registerPlayer, togglePlay, loadStream, stopStream, log,
  }
})
