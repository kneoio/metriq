import { ref } from 'vue'
import { defineStore } from 'pinia'

export const useTracesStore = defineStore('traces', () => {
  const selectedBrand   = ref('all')
  const selectedTraceId = ref<string | null>(null)
  const showFlowTiming  = ref(false)
  /** Shared across Traces / Cron / Independent: preset group id → exact `code` match. */
  const eventCodeFilterGroupId = ref('')
  return { selectedBrand, selectedTraceId, showFlowTiming, eventCodeFilterGroupId }
})
