<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useMetriqStore, METRIC_EVENT_TYPES } from '@/stores/metriq'
import { useConnectionStore } from '@/stores/connection'
import { useTracesStore }    from '@/stores/traces'
import { useStationsStore }  from '@/stores/stations'
import { SERVICE_OPTIONS, metricEventFilterClass } from '@/utils/service'
import { relTime }           from '@/utils/time'
import SystemDashboardView from '@/views/SystemDashboardView.vue'
import StreamView      from '@/views/StreamView.vue'
import TracesView      from '@/views/TracesView.vue'
import CronView        from '@/views/CronView.vue'
import IndependentView from '@/views/IndependentView.vue'
import DashboardView from '@/views/DashboardView.vue'
import AgendaView    from '@/views/AgendaView.vue'

const appVersion = __APP_VERSION__
const buildTime  = __BUILD_TIME__

const metriq   = useMetriqStore()
const conn     = useConnectionStore()
const traces   = useTracesStore()
const stations = useStationsStore()

const clockNow = ref(Date.now())
setInterval(() => { clockNow.value = Date.now() }, 1000)

const stationClock = computed(() => {
  const tz      = stations.timezoneByStation[stations.activeStation]
  const country = stations.countryByStation[stations.activeStation]
  if (!tz) return null
  try {
    const now  = new Date(clockNow.value)
    const time = new Intl.DateTimeFormat('en-GB', {
      timeZone: tz, hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false
    }).format(now)
    const offsetRaw = new Intl.DateTimeFormat('en', {
      timeZone: tz, timeZoneName: 'shortOffset'
    }).formatToParts(now).find(p => p.type === 'timeZoneName')?.value ?? ''
    const offset = offsetRaw.replace('GMT', '') || '+0'
    return { time, offset, country: country ?? '' }
  } catch { return null }
})

// Traces sidebar: filtered to active station
const tracesForSelectedBrand = computed(() => {
  const allEntries = Object.entries(metriq.byTrace)
  return allEntries
    .map(([id, evts]) => {
      const brandEvts = (evts as any[]).filter((e: any) =>
        (e.data.brandName   ?? '').trim() === stations.activeStation &&
        (e.data.processType ?? '').toUpperCase() === 'FLOW'
      )
      return { id, count: brandEvts.length, lastTime: brandEvts[brandEvts.length - 1]?.receivedAt as Date | undefined }
    })
    .filter(t => t.count > 0)
    .sort((a, b) => (b.lastTime?.getTime() ?? 0) - (a.lastTime?.getTime() ?? 0))
})

watch(() => stations.activeStation, (station) => {
  traces.selectedTraceId = null
  document.title = station ? `${station} - Metriq` : 'Metriq'
}, { immediate: true })

const streamViewRef = ref<InstanceType<typeof StreamView> | null>(null)
function clearAll() { (streamViewRef.value as any)?.clearAll?.() }

const topbarTitle = computed(() => {
  if (stations.topView === 'metrics') return 'ALL METRICS'
  if (stations.topView === 'system-dashboard') return 'SYSTEM STATE'
  const labels: Record<string, string> = {
    dashboard:   stations.activeStation.toUpperCase(),
    agenda:      'AGENDA',
    traces:      'TRACES',
    cron:        'CRON',
    independent: 'INDEPENDENT',

  }
  return labels[stations.activeStationView] ?? ''
})

onMounted(() => conn.connect())
onUnmounted(() => conn.disconnect())
</script>

<template>
  <div class="layout">

    <!-- ── Sidebar ── -->
    <aside class="sidebar">
      <div class="logo">
        <div class="logo-text">METRIQ</div>
        <div class="logo-sub">event stream</div>
      </div>

      <nav class="nav-menu">
        <!-- Global views -->
        <div class="nav-item" :class="{ active: stations.topView === 'metrics' }" @click="stations.goToMetrics()">
          <span class="nav-dot"></span>All Metrics
        </div>
        <div class="nav-item" :class="{ active: stations.topView === 'system-dashboard' }" @click="stations.goToSystemDashboard()">
          <span class="nav-dot"></span>System State
        </div>

        <div class="sidebar-divider"></div>

        <!-- One section per station -->
        <div v-for="station in stations.stationList" :key="station" class="station-section">
          <div class="station-header"
            :class="{ active: stations.topView === 'station' && stations.activeStation === station }"
            @click="stations.goToStation(station)">
            <span class="station-chevron"
              :class="{ open: stations.topView === 'station' && stations.activeStation === station }">›</span>
            {{ station }}
          </div>
          <template v-if="stations.topView === 'station' && stations.activeStation === station">
            <div class="nav-sub-item" :class="{ active: stations.activeStationView === 'dashboard' }" @click="stations.goToView('dashboard')">
              <span class="nav-dot"></span>Dashboard
            </div>
            <div class="nav-sub-item" :class="{ active: stations.activeStationView === 'agenda' }" @click="stations.goToView('agenda')">
              <span class="nav-dot"></span>Agenda
            </div>
            <div class="nav-sub-item" :class="{ active: stations.activeStationView === 'traces' }" @click="stations.goToView('traces')">
              <span class="nav-dot"></span>Traces
            </div>
            <div class="nav-sub-item" :class="{ active: stations.activeStationView === 'cron' }" @click="stations.goToView('cron')">
              <span class="nav-dot"></span>Cron
            </div>
            <div class="nav-sub-item" :class="{ active: stations.activeStationView === 'independent' }" @click="stations.goToView('independent')">
              <span class="nav-dot"></span>Independent
            </div>
          </template>
        </div>
      </nav>

      <!-- ── Sidebar secondary content ── -->

      <!-- Metrics: filters -->
      <template v-if="stations.topView === 'metrics'">
        <div class="sidebar-divider"></div>
        <div class="filter-section">
          <div class="filter-label">Filter by type</div>
          <div class="filter-tags">
            <span class="filter-tag" :class="{ active: metriq.activeTypeFilters.size === 0 }" @click="metriq.clearTypeFilters()">all</span>
            <span v-for="type in METRIC_EVENT_TYPES" :key="type" class="filter-tag"
              :class="[metricEventFilterClass(type), { active: metriq.activeTypeFilters.has(type) }]"
              @click="metriq.toggleTypeFilter(type)">{{ type.toLowerCase().replace(/_/g, ' ') }}</span>
          </div>
        </div>
        <div class="sidebar-divider"></div>
        <div class="filter-section">
          <div class="filter-label">Filter by brand</div>
          <div class="filter-tags">
            <span class="filter-tag brand-tag" :class="{ active: metriq.activeBrandFilter === 'all' }" @click="metriq.setBrandFilter('all')">all</span>
            <span v-for="brand in metriq.knownBrands" :key="brand" class="filter-tag brand-tag"
              :class="{ active: metriq.activeBrandFilter === brand }" @click="metriq.setBrandFilter(brand)">{{ brand.toLowerCase() }}</span>
          </div>
        </div>
        <div class="sidebar-divider"></div>
        <div class="filter-section">
          <div class="filter-label">Filter by service</div>
          <div class="filter-tags">
            <span v-for="svc in SERVICE_OPTIONS" :key="svc.value" class="filter-tag"
              :class="[svc.cls, { active: metriq.activeServiceFilter === svc.value }]"
              @click="metriq.setServiceFilter(svc.value)">{{ svc.label }}</span>
          </div>
        </div>
      </template>

      <!-- Traces: trace list -->
      <template v-else-if="stations.topView === 'station' && stations.activeStationView === 'traces'">
        <div class="sidebar-divider"></div>
        <div class="trace-list">
          <div v-if="tracesForSelectedBrand.length === 0" class="sidebar-empty">no traces</div>
          <div v-for="t in tracesForSelectedBrand" :key="t.id" class="trace-item"
            :class="{ active: traces.selectedTraceId === t.id, multi: t.count > 1 }"
            @click="traces.selectedTraceId = t.id">
            <div class="trace-item-id">{{ t.id }}</div>
            <div class="trace-item-meta">
              <span class="trace-count-badge" :class="{ 'multi-badge': t.count > 1 }">{{ t.count }}</span>
              <span class="trace-time-label">{{ relTime(t.lastTime) }}</span>
              <button class="trace-delete-btn" title="Delete trace"
                @click.stop="metriq.deleteTrace(t.id); if (traces.selectedTraceId === t.id) traces.selectedTraceId = null">🗑</button>
            </div>
          </div>
        </div>
      </template>

      <!-- ── Version ── -->
      <div style="padding:4px 12px;font-family:var(--mono);font-size:0.55rem;color:var(--text-muted);opacity:0.5;margin-top:auto;">
        v{{ appVersion }} · {{ buildTime }}
      </div>

      <!-- ── WS status ── -->
      <div class="status-indicator">
        <div class="status-row">
          <div class="status-dot" :class="conn.status"></div>
          <span class="status-text">{{ conn.status }}</span>
          <button class="reconnect-btn" @click="conn.reconnect()" title="Reconnect">↺</button>
        </div>
      </div>
    </aside>

    <!-- ── Topbar ── -->
    <header class="topbar">
      <div class="topbar-left">
        <span class="topbar-title">{{ topbarTitle }}</span>
      </div>
      <div class="topbar-center">
        <template v-if="stationClock">
          <span class="topbar-clock">{{ stationClock.time }}</span>
          <span class="topbar-clock-meta">{{ stationClock.country }} {{ stationClock.offset }}</span>
        </template>
      </div>
      <div class="topbar-right">
        <div class="topbar-stats">
          <span class="topbar-stat"><span class="tstat-label">rcvd</span><span class="tstat-value">{{ metriq.totalCount }}</span></span>
          <span class="topbar-stat"><span class="tstat-label">/min</span><span class="tstat-value amber">{{ metriq.rateCount }}</span></span>
          <span class="topbar-stat"><span class="tstat-label">err</span><span class="tstat-value red">{{ metriq.errorCount }}</span></span>
        </div>
        <div class="topbar-sep"></div>
        <button v-if="stations.topView === 'metrics'" class="clear-btn" @click="clearAll">CLEAR</button>
        <div class="live-badge">
          <div class="live-dot" v-show="conn.status === 'connected'"></div>
          <span>{{ conn.status === 'connected' ? 'LIVE' : conn.status.toUpperCase() }}</span>
        </div>
      </div>
    </header>

    <!-- ── Active view ── -->
    <StreamView           v-if="stations.topView === 'metrics'"          ref="streamViewRef" />
    <SystemDashboardView  v-else-if="stations.topView === 'system-dashboard'" />
    <DashboardView v-else-if="stations.activeStationView === 'dashboard'" />
    <AgendaView    v-else-if="stations.activeStationView === 'agenda'" />
    <TracesView      v-else-if="stations.activeStationView === 'traces'" />
    <CronView        v-else-if="stations.activeStationView === 'cron'" />
    <IndependentView v-else-if="stations.activeStationView === 'independent'" />

  </div>
</template>

<style scoped>
/* ── Topbar left / center / right ── */
.topbar-left  { display: flex; align-items: center; gap: 14px; flex: 1; }
.topbar-right { display: flex; align-items: center; gap: 8px; flex: 1; justify-content: flex-end; }

/* ── Topbar stats ── */
.topbar-stats { display: flex; align-items: center; gap: 10px; }
.topbar-stat  { display: flex; flex-direction: column; align-items: flex-end; gap: 0; line-height: 1.1; }
.tstat-label  { font-family: var(--mono); font-size: 0.5rem; letter-spacing: 1.5px; color: var(--text-muted); text-transform: uppercase; }
.tstat-value  { font-family: var(--mono); font-size: 0.85rem; font-weight: 600; color: var(--accent); line-height: 1; }
.tstat-value.amber { color: var(--amber, #f5a623); }
.tstat-value.red   { color: var(--accent3, #fa6d6d); }

.topbar-sep { width: 1px; height: 14px; background: var(--border, #333); margin: 0 2px; }
.topbar-center { position: absolute; left: 50%; transform: translateX(-50%); display: flex; align-items: baseline; gap: 8px; }
.topbar-clock { font-family: var(--mono); font-size: 0.85rem; font-weight: 600; letter-spacing: 2px; color: var(--accent2); }
.topbar-clock-meta { font-family: var(--mono); font-size: 0.58rem; letter-spacing: 1px; color: var(--text-dim); }

/* ── Station nav ── */
.station-section { display: flex; flex-direction: column; }

.station-header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 5px 12px;
  font-family: var(--mono);
  font-size: 0.65rem;
  letter-spacing: 0.5px;
  color: var(--text-muted);
  cursor: pointer;
  transition: color 0.15s;
  user-select: none;
}
.station-header:hover  { color: var(--text); }
.station-header.active { color: var(--accent); }

.station-chevron {
  display: inline-block;
  font-size: 0.7rem;
  transition: transform 0.2s;
  color: var(--text-dim);
  flex-shrink: 0;
}
.station-chevron.open { transform: rotate(90deg); }

.nav-sub-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 12px 4px 26px;
  font-family: var(--mono);
  font-size: 0.62rem;
  letter-spacing: 0.5px;
  color: var(--text-muted);
  cursor: pointer;
  transition: color 0.15s, background 0.15s;
}
.nav-sub-item:hover  { color: var(--text); background: rgba(255,255,255,0.03); }
.nav-sub-item.active { color: var(--accent); }
</style>
