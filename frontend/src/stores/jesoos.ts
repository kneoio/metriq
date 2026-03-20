import { ref } from 'vue'
import { defineStore } from 'pinia'

export const useJesoosStore = defineStore('jesoos', () => {
  const brand  = ref('lumisonic')
  const status = ref('idle')
  return { brand, status }
})
