import { ref } from 'vue'
import { defineStore } from 'pinia'
import { STATION_LIST } from '@/utils/service'

/**
 * Global brand context — single source of truth for the active brand.
 * All commands (aivox, jesoos), the player, and filtered views read from here.
 * "All Metrics" is the only view that stays globally unfiltered.
 */
export const useContextStore = defineStore('context', () => {
  const activeBrand = ref<string>(STATION_LIST[0])
  return { activeBrand }
})
