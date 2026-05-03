import { ref } from 'vue'
import { defineStore } from 'pinia'

export const useTracesStore = defineStore('traces', () => {
  const selectedBrand   = ref('all')
  const selectedTraceId = ref<string | null>(null)
  const showFlowTiming  = ref(false)
  return { selectedBrand, selectedTraceId, showFlowTiming }
})
