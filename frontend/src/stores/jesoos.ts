import { ref, computed, reactive, watch } from 'vue'
import { defineStore } from 'pinia'
import { useContextStore } from '@/stores/context'

export const useJesoosStore = defineStore('jesoos', () => {

  const context    = useContextStore()
  const cmdStatus        = ref('')
  const cmdResult        = ref<unknown>(null)
  const djByBrand        = reactive<Record<string, boolean | null>>({})
  const liveByBrand      = reactive<Record<string, boolean | null>>({})

  const djEnabled = computed(() => {
    const b = context.activeBrand
    return b in djByBrand ? djByBrand[b] : null
  })

  const liveStatus = computed(() => {
    const b = context.activeBrand
    return b in liveByBrand ? liveByBrand[b] : null
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

  async function pollLiveStatus() {
    const brand = context.activeBrand
    try {
      const res = await fetch(`/jesoos/info/${brand}/live`)
      if (!res.ok) { liveByBrand[brand] = null; return }
      const text = (await res.text()).trim().toLowerCase()
      liveByBrand[brand] = text === 'true'
    } catch {
      liveByBrand[brand] = null
    }
  }

  pollDjStatus()
  setInterval(pollDjStatus, 60_000)

  pollLiveStatus()
  setInterval(pollLiveStatus, 30_000)

  watch(() => context.activeBrand, () => { pollDjStatus(); pollLiveStatus() })

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

  const start     = () => command('start')
  const stop      = () => command('stop')
  const enableDj  = () => command('enable-dj')
  const disableDj = () => command('disable-dj')

  return { cmdStatus, cmdResult, djByBrand, djEnabled, liveStatus, start, stop, enableDj, disableDj }
})
