import { ref, computed, reactive, watch, onUnmounted } from 'vue'
import { defineStore } from 'pinia'
import { useContextStore } from '@/stores/context'

export const useJesoosStore = defineStore('jesoos', () => {

  const context    = useContextStore()
  const cmdStatus   = ref('')
  const cmdResult   = ref<unknown>(null)
  const djByBrand   = reactive<Record<string, boolean | null>>({})

  const djEnabled = computed(() => {
    const b = context.activeBrand
    return b in djByBrand ? djByBrand[b] : null
  })

  async function pollDjStatus() {
    const brand = context.activeBrand
    try {
      const res = await fetch(`/jesoos/info/${brand}/dj-status`)
      if (!res.ok) { djByBrand[brand] = null; return }
      const text = (await res.text()).trim().toLowerCase()
      djByBrand[brand] = text === 'true'
    } catch {
      djByBrand[brand] = null
    }
  }

  pollDjStatus()
  const _djPoll = setInterval(pollDjStatus, 60_000)
  onUnmounted(() => clearInterval(_djPoll))

  watch(() => context.activeBrand, pollDjStatus)

  async function command(cmd: string) {
    cmdStatus.value = 'pending'
    cmdResult.value = null
    try {
      const res  = await fetch(`/jesoos/command/${context.activeBrand}/${cmd}`, { method: 'POST', headers: { 'Content-Type': 'application/json' } })
      const data = await res.json()
      cmdResult.value = data
      cmdStatus.value = res.ok ? 'ok' : 'err'
    } catch (e: any) {
      cmdResult.value = e.message
      cmdStatus.value = 'err'
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
    } catch (e: any) {
      cmdResult.value = e.message
      cmdStatus.value = 'err'
    }
  }

  const start     = () => command('start')
  const stop      = () => command('stop')
  const enableDj  = () => command('enable-dj')
  const disableDj = () => command('disable-dj')

  return { cmdStatus, cmdResult, djByBrand, djEnabled, start, stop, stopAll, enableDj, disableDj }
})
