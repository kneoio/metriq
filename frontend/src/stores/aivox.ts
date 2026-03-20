import { ref } from 'vue'
import { defineStore } from 'pinia'

export const useAivoxStore = defineStore('aivox', () => {
  const station      = ref('lumisonic')
  const fragCount    = ref(0)
  const lastFragSize = ref('—')
  const errorCount   = ref(0)
  const status       = ref('idle')
  return { station, fragCount, lastFragSize, errorCount, status }
})
