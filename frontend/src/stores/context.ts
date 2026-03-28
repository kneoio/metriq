import { computed } from 'vue'
import { defineStore } from 'pinia'
import { useStationsStore } from '@/stores/stations'

/**
 * Thin alias — delegates to the stations store.
 * Keeps aivox, jesoos, and TracesView working without changes.
 */
export const useContextStore = defineStore('context', () => {
  const stations    = useStationsStore()
  const activeBrand = computed(() => stations.activeStation)
  return { activeBrand }
})
