import { ref, watch, onUnmounted } from 'vue'
import { defineStore } from 'pinia'
import { useContextStore } from '@/stores/context'

export const useJesoosStore = defineStore('jesoos', () => {

  const context    = useContextStore()
  const status     = ref('idle')
  const cmdStatus  = ref('')
  const cmdResult  = ref<unknown>(null)
  const djEnabled  = ref<boolean | null>(null)

  async function pollDjStatus() {
    try {
      const res = await fetch(`/jesoos/info/${context.activeBrand}/dj-status`)
      if (!res.ok) { djEnabled.value = null; return }
      const text = (await res.text()).trim().toLowerCase()
      djEnabled.value = text === 'true'
    } catch {
      djEnabled.value = null
    }
  }

  pollDjStatus()
  const _djPoll = setInterval(pollDjStatus, 60_000)
  onUnmounted(() => clearInterval(_djPoll))

  watch(() => context.activeBrand, () => {
    djEnabled.value = null
    pollDjStatus()
  })

  async function command(cmd: string) {
    cmdStatus.value = 'pending'
    cmdResult.value = null
    try {
      const res  = await fetch(`/jesoos/command/${context.activeBrand}/${cmd}`, { method: 'POST', headers: { 'Content-Type': 'application/json' } })
      const data = await res.json()
      cmdResult.value = data
      cmdStatus.value = res.ok ? 'ok' : 'err'
      if (cmd === 'start') status.value = res.ok ? 'running' : 'error'
      if (cmd === 'stop')  status.value = res.ok ? 'idle'    : 'error'
    } catch (e: any) {
      cmdResult.value = e.message
      cmdStatus.value = 'err'
      status.value    = 'error'
    }
  }

  async function stopAll() {
    cmdStatus.value = 'pending'
    cmdResult.value = null
    try {
      const res  = await fetch('/jesoos/stop-all', { method: 'POST', headers: { 'Content-Type': 'application/json' } })
      const data = await res.json()
      cmdResult.value = data
      cmdStatus.value = res.ok ? 'ok' : 'err'
      if (res.ok) status.value = 'idle'
    } catch (e: any) {
      cmdResult.value = e.message
      cmdStatus.value = 'err'
      status.value    = 'error'
    }
  }

  const start     = () => command('start')
  const stop      = () => command('stop')
  const enableDj  = () => command('enable-dj')
  const disableDj = () => command('disable-dj')

  return { status, cmdStatus, cmdResult, djEnabled, start, stop, stopAll, enableDj, disableDj }
})
