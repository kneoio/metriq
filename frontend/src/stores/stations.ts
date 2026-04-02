import { ref, reactive } from 'vue'
import { defineStore } from 'pinia'
import { STATION_LIST } from '@/utils/service'

export type StationView = 'dashboard' | 'agenda' | 'traces' | 'cron' | 'independent' | 'playlist'
export type TopView     = 'metrics' | 'system-dashboard' | 'station'

export const useStationsStore = defineStore('stations', () => {
  // Ready for dynamic loading later — just swap stationList.value
  const stationList        = ref<string[]>([...STATION_LIST])
  const activeStation      = ref<string>(stationList.value[0])
  const activeStationView  = ref<StationView>('dashboard')
  const topView            = ref<TopView>('metrics')
  const timezoneByStation  = reactive<Record<string, string>>({})
  const countryByStation   = reactive<Record<string, string>>({})

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

  function setTimezone(station: string, tz: string, country?: string) {
    timezoneByStation[station] = tz
    if (country) countryByStation[station] = country
  }

  return { stationList, activeStation, activeStationView, topView, timezoneByStation, countryByStation, goToMetrics, goToSystemDashboard, goToStation, goToView, setTimezone }
})
