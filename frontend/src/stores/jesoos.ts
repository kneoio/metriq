import { ref } from 'vue'
import { defineStore } from 'pinia'
import { useContextStore } from '@/stores/context'

export const useJesoosStore = defineStore('jesoos', () => {

  const context    = useContextStore()
  const status     = ref('idle')
  const cmdStatus  = ref('')
  const cmdResult  = ref<unknown>(null)

  async function command(cmd: string) {
    cmdStatus.value = 'pending'
    cmdResult.value = null
    try {
      const res  = await fetch(`/jesoos/${context.activeBrand}/${cmd}`, { method: 'POST', headers: { 'Content-Type': 'application/json' } })
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

  const start     = () => command('start')
  const stop      = () => command('stop')
  const enableDj  = () => command('enabledj')
  const disableDj = () => command('disabledj')

  return { status, cmdStatus, cmdResult, start, stop, enableDj, disableDj }
})
