import { ref } from 'vue'
import { defineStore } from 'pinia'
import { STATION_LIST } from '@/utils/service'

export type StationView = 'dashboard' | 'agenda' | 'traces' | 'cron' | 'independent'
export type TopView     = 'metrics' | 'system-dashboard' | 'station'

export const useStationsStore = defineStore('stations', () => {
  // Ready for dynamic loading later — just swap stationList.value
  const stationList        = ref<string[]>([...STATION_LIST])
  const activeStation      = ref<string>(stationList.value[0])
  const activeStationView  = ref<StationView>('dashboard')
  const topView            = ref<TopView>('metrics')

  function goToMetrics() {
    topView.value = 'metrics'
  }

  function goToSystemDashboard() {
    topView.value = 'system-dashboard'
  }

  function goToStation(station: string, view: StationView = 'dashboard') {
    activeStation.value     = station
    activeStationView.value = view
    topView.value           = 'station'
  }

  function goToView(view: StationView) {
    activeStationView.value = view
    topView.value           = 'station'
  }

  return { stationList, activeStation, activeStationView, topView, goToMetrics, goToSystemDashboard, goToStation, goToView }
})
