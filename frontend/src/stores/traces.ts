import { ref } from 'vue'
import { defineStore } from 'pinia'

export const useTracesStore = defineStore('traces', () => {
  const selectedBrand   = ref('all')
  const selectedTraceId = ref<string | null>(null)
  const showFlowTiming  = ref(false)
  /** Shared across Traces / Cron / Independent: substring OR filter on `event.data.code`. */
  const eventCodeFilterText = ref('')
  return { selectedBrand, selectedTraceId, showFlowTiming, eventCodeFilterText }
})
