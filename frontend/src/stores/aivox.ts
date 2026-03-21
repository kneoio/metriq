import { ref } from 'vue'
import { defineStore } from 'pinia'

export const useAivoxStore = defineStore('aivox', () => {
  const station      = ref('lumisonic')
  const fragCount    = ref(0)
  const lastFragSize = ref('—')
  const errorCount   = ref(0)
  const status       = ref('idle')
  const cmdStatus    = ref('')   // last server command result

  async function serverAction(method: 'POST' | 'DELETE') {
    cmdStatus.value = method === 'POST' ? 'starting…' : 'stopping…'
    try {
      const r    = await fetch('/aivox/' + station.value + '/command/', { method })
      const text = await r.text()
      cmdStatus.value = r.ok ? (method === 'POST' ? 'started' : 'stopped') : 'error: ' + text
    } catch (e: any) {
      cmdStatus.value = 'error: ' + e.message
    }
  }

  return { station, fragCount, lastFragSize, errorCount, status, cmdStatus, serverAction }
})
