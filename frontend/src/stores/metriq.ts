import { ref, computed, reactive } from 'vue'
import { defineStore } from 'pinia'
import type { MetricEventData, EventEntry } from '@/types'
import { isError } from '@/utils/service'

// Shape returned by GET /metriq/api/snapshot
interface Snapshot {
  events:  MetricEventData[]
  byBrand: Record<string, MetricEventData[]>
  byTrace: Record<string, MetricEventData[]>
}

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

  // ── Helpers ────────────────────────────────────────────────────────────────
  function makeEntry(data: MetricEventData): EventEntry {
    // _receivedAt is stamped by the BE EventStore; fall back to now
    const ts = (data as any)._receivedAt
    const receivedAt = ts ? new Date(ts) : new Date()
    return { data, receivedAt, id: receivedAt.getTime() + Math.random() }
  }

  function registerMeta(data: MetricEventData) {
    const type  = (data.type      ?? 'UNKNOWN').toUpperCase()
    const brand = (data.brandName ?? '').trim()
    if (!knownTypes.value.includes(type))   knownTypes.value.push(type)
    if (brand && !knownBrands.value.includes(brand)) knownBrands.value.push(brand)
  }

  // ── Actions ────────────────────────────────────────────────────────────────

  /**
   * Seed the store from a BE snapshot (called once on WS connect).
   * Completely replaces existing state so the FE stays thin and in sync.
   */
  function seedFromSnapshot(snapshot: Snapshot) {
    // Clear
    events.value = []
    totalCount.value = 0
    errorCount.value = 0
    recentTimestamps.value = []
    knownTypes.value = []
    knownBrands.value = []
    Object.keys(byBrand).forEach(k => delete byBrand[k])
    Object.keys(byTrace).forEach(k => delete byTrace[k])

    // Seed global event list (newest first from BE)
    ;(snapshot.events ?? []).forEach(data => {
      registerMeta(data)
      if (isError(data.type)) errorCount.value++
      events.value.push(makeEntry(data))
      totalCount.value++
    })

    // Seed byBrand (oldest-first lists from BE)
    Object.entries(snapshot.byBrand ?? {}).forEach(([brand, evts]) => {
      byBrand[brand] = evts.map(makeEntry)
      if (!knownBrands.value.includes(brand)) knownBrands.value.push(brand)
    })

    // Seed byTrace
    Object.entries(snapshot.byTrace ?? {}).forEach(([traceId, evts]) => {
      byTrace[traceId] = evts.map(makeEntry)
    })
  }

  /** Live event from WebSocket — appended on top of existing state. */
  function ingestEvent(data: MetricEventData) {
    const now = Date.now()
    totalCount.value++
    recentTimestamps.value.push(now)
    recentTimestamps.value = recentTimestamps.value.filter(t => now - t < 60_000)

    if (isError(data.type)) errorCount.value++
    registerMeta(data)

    const entry = makeEntry(data)

    // ── global list ──
    events.value.unshift(entry)
    if (events.value.length > 120) events.value.pop()
    const cutoff = now - 15 * 60 * 1000
    events.value = events.value.filter(e => e.receivedAt.getTime() > cutoff)

    // ── byBrand ──
    const brand = (data.brandName ?? '').trim()
    if (brand) {
      if (!byBrand[brand]) byBrand[brand] = []
      byBrand[brand].push(entry)
      if (byBrand[brand].length > 500) byBrand[brand].shift()
    }

    // ── byTrace ──
    const traceId = String(data.traceId ?? '').trim()
    if (traceId && traceId !== 'null') {
      if (!byTrace[traceId]) byTrace[traceId] = []
      byTrace[traceId].push(entry)
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

  async function deleteTrace(traceId: string) {
    try {
      await fetch(`/metriq/api/traces/${encodeURIComponent(traceId)}`, { method: 'DELETE' })
    } catch { /* best-effort */ }
    delete byTrace[traceId]
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
    seedFromSnapshot, ingestEvent, clearAllEvents, deleteTrace,
    setFilter, setBrandFilter, setServiceFilter,
  }
})
