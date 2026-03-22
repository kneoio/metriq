import { ref } from 'vue'
import { defineStore } from 'pinia'

export const useJesoosStore = defineStore('jesoos', () => {
  const brand       = ref('lumisonic')
  const status      = ref('idle')
  const cmdStatus   = ref('')          // 'pending' | 'ok' | 'err' | ''
  const startResult = ref<unknown>(null)

  async function start() {
    cmdStatus.value = 'pending'
    status.value    = 'running'
    try {
      const res  = await fetch(`/jesoos/${brand.value}/start`, { method: 'POST', headers: { 'Content-Type': 'application/json' } })
      const data = await res.json()
      startResult.value = data
      cmdStatus.value   = res.ok ? 'ok' : 'err'
      status.value      = res.ok ? 'running' : 'error'
    } catch (e: any) {
      startResult.value = e.message
      cmdStatus.value   = 'err'
      status.value      = 'error'
    }
  }

  return { brand, status, cmdStatus, startResult, start }
})
