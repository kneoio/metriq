import { ref, computed, reactive } from 'vue'
import { defineStore } from 'pinia'
import type { MetricEventData, EventEntry } from '@/types'
import { isError } from '@/utils/service'

export const useMetriqStore = defineStore('metriq', () => {
  // ── State ──────────────────────────────────────────────────────────────────
  const events              = ref<EventEntry[]>([])
  const totalCount          = ref(0)
  const errorCount          = ref(0)
  const recentTimestamps    = ref<number[]>([])
  const activeFilter        = ref('all')
  const activeBrandFilter   = ref('all')
  const activeServiceFilter = ref('all')
  const knownTypes          = ref<string[]>([])
  const knownBrands         = ref<string[]>([])

  // byBrand[brandSlug] = EventEntry[] — all events for a brand across all traces
  // byTrace[traceId]   = EventEntry[] — full cross-service trace flow
  const byBrand = reactive<Record<string, EventEntry[]>>({})
  const byTrace = reactive<Record<string, EventEntry[]>>({})

  // ── Computed ───────────────────────────────────────────────────────────────
  const rateCount = computed(() => recentTimestamps.value.length)

  const filteredEvents = computed(() =>
    events.value.filter(entry => {
      const type    = (entry.data.type      ?? 'UNKNOWN').toUpperCase()
      const brand   = (entry.data.brandName ?? '').trim()
      const service = (entry.data.serviceId ?? '').trim()
      return (
        (activeFilter.value        === 'all' || activeFilter.value        === type)    &&
        (activeBrandFilter.value   === 'all' || activeBrandFilter.value   === brand)   &&
        (activeServiceFilter.value === 'all' || activeServiceFilter.value === service)
      )
    })
  )

  // ── Actions ────────────────────────────────────────────────────────────────
  function ingestEvent(data: MetricEventData) {
    const now = Date.now()
    totalCount.value++
    recentTimestamps.value.push(now)
    recentTimestamps.value = recentTimestamps.value.filter(t => now - t < 60_000)

    if (isError(data.type)) errorCount.value++

    const type = (data.type ?? 'UNKNOWN').toUpperCase()
    if (!knownTypes.value.includes(type)) knownTypes.value.push(type)

    const brand = (data.brandName ?? '').trim()
    if (brand && !knownBrands.value.includes(brand)) knownBrands.value.push(brand)

    const entry: EventEntry = { data, receivedAt: new Date(), id: now + Math.random() }
    events.value.unshift(entry)
    if (events.value.length > 120) events.value.pop()

    // Drop events older than 15 min
    const cutoff = now - 15 * 60 * 1000
    events.value = events.value.filter(e => e.receivedAt.getTime() > cutoff)

    // ── Feed byBrand ──
    if (brand) {
      if (!byBrand[brand]) byBrand[brand] = []
      byBrand[brand].push(entry)
      if (byBrand[brand].length > 500) byBrand[brand].shift()
    }

    // ── Feed byTrace ──
    const traceId = String(data.traceId ?? '').trim()
    if (traceId) {
      if (!byTrace[traceId]) byTrace[traceId] = []
      byTrace[traceId].push(entry)
      // Cap total traces at 200 — evict oldest
      const keys = Object.keys(byTrace)
      if (keys.length > 200) {
        const oldest = keys.reduce((a, b) =>
          (byTrace[a][0]?.receivedAt?.getTime() ?? 0) <
          (byTrace[b][0]?.receivedAt?.getTime() ?? 0) ? a : b
        )
        delete byTrace[oldest]
      }
    }
  }

  function clearAllEvents() {
    events.value = []
    totalCount.value = 0
    errorCount.value = 0
    recentTimestamps.value = []
    knownBrands.value = []
    activeFilter.value = 'all'
    activeBrandFilter.value = 'all'
    activeServiceFilter.value = 'all'
    // Clear reactive maps
    Object.keys(byBrand).forEach(k => delete byBrand[k])
    Object.keys(byTrace).forEach(k => delete byTrace[k])
  }

  function setFilter(f: string)        { activeFilter.value        = f }
  function setBrandFilter(b: string)   { activeBrandFilter.value   = b }
  function setServiceFilter(s: string) { activeServiceFilter.value = s }

  return {
    events, totalCount, errorCount, recentTimestamps,
    activeFilter, activeBrandFilter, activeServiceFilter,
    knownTypes, knownBrands, byBrand, byTrace,
    rateCount, filteredEvents,
    ingestEvent, clearAllEvents, setFilter, setBrandFilter, setServiceFilter,
  }
})
