<script setup lang="ts">
import { computed, onMounted, onUnmounted } from 'vue'
import { useMetriqStore } from '@/stores/metriq'
import { useConnectionStore } from '@/stores/connection'
import { useAivoxStore  } from '@/stores/aivox'
import { useJesoosStore } from '@/stores/jesoos'
import { useTracesStore } from '@/stores/traces'
import { SERVICE_OPTIONS, STATION_LIST } from '@/utils/service'
import { relTime } from '@/utils/time'
import StreamView from '@/views/StreamView.vue'
import TracesView from '@/views/TracesView.vue'
import AivoxView  from '@/views/AivoxView.vue'
import JesoosView from '@/views/JesoosView.vue'

// Stores
const metriq = useMetriqStore()
const conn   = useConnectionStore()
const aivox  = useAivoxStore()
const jesoos = useJesoosStore()
const traces = useTracesStore()

// Derive the active view from connection store (single source of truth nav)
import { ref, watch } from 'vue'
type View = 'stream' | 'traces' | 'player' | 'jesoos'
const activeView = ref<View>('stream')

// Traces sidebar computed
const traceBrands = computed(() => Object.keys(metriq.byBrand))

const tracesForSelectedBrand = computed(() => {
  const allEntries = Object.entries(metriq.byTrace)
  const filtered = (!traces.selectedBrand || traces.selectedBrand === 'all')
    ? allEntries
    : allEntries.filter(([, evts]) =>
        (evts as any[]).some((e: any) => (e.data.brandName ?? '').trim() === traces.selectedBrand)
      )
  return filtered
    .map(([id, evts]) => ({
      id,
      count: (evts as any[]).length,
      lastTime: (evts as any[])[(evts as any[]).length - 1]?.receivedAt as Date | undefined,
    }))
    .sort((a, b) => (b.lastTime?.getTime() ?? 0) - (a.lastTime?.getTime() ?? 0))
})

// Reset trace selection when brand filter changes
watch(() => traces.selectedBrand, () => { traces.selectedTraceId = null })

// Stream sidebar: GSAP display values live in StreamView, but we need a ref to call clearAll
const streamViewRef = ref<InstanceType<typeof StreamView> | null>(null)

// Stream display totals are exposed from StreamView
const displayTotal  = computed(() => (streamViewRef.value as any)?.displayTotal ?? 0)
const displayRate   = computed(() => (streamViewRef.value as any)?.displayRate  ?? 0)
const displayErrors = computed(() => (streamViewRef.value as any)?.displayErrors ?? 0)

function clearAll() { (streamViewRef.value as any)?.clearAll?.() }

const topbarTitle = computed(() => ({
  stream: 'ALL METRICS',
  traces: 'TRACE FLOW',
  player: 'AIVOX CONTROL',
  jesoos: 'JESOOS CONTROL',
}[activeView.value]))

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
        <div class="nav-item" :class="{ active: activeView === 'stream' }" @click="activeView = 'stream'">
          <span class="nav-dot"></span>All Metrics
        </div>
        <div class="nav-item" :class="{ active: activeView === 'traces' }" @click="activeView = 'traces'">
          <span class="nav-dot"></span>Traces
        </div>
        <div class="nav-item" :class="{ active: activeView === 'player' }" @click="activeView = 'player'">
          <span class="nav-dot"></span>Aivox
        </div>
        <div class="nav-item" :class="{ active: activeView === 'jesoos' }" @click="activeView = 'jesoos'">
          <span class="nav-dot"></span>Jesoos
        </div>
      </nav>

      <!-- ── Stream sidebar ── -->
      <template v-if="activeView === 'stream'">
        <div class="sidebar-stats">
          <div class="stat-item">
            <span class="stat-label">total received</span>
            <span class="stat-value">{{ displayTotal }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">last minute</span>
            <span class="stat-value amber">{{ displayRate }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">error types</span>
            <span class="stat-value red">{{ displayErrors }}</span>
          </div>
        </div>
        <div class="sidebar-divider"></div>
        <div class="filter-section">
          <div class="filter-label">Filter by type</div>
          <div class="filter-tags">
            <span class="filter-tag" :class="{ active: metriq.activeFilter === 'all' }" @click="metriq.setFilter('all')">all</span>
            <span v-for="type in metriq.knownTypes" :key="type" class="filter-tag"
              :class="{ active: metriq.activeFilter === type }" @click="metriq.setFilter(type)">{{ type.toLowerCase() }}</span>
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
          <div class="filter-label">Quick start</div>
          <div class="select-wrap" style="margin-bottom:6px;">
            <select class="station-select" v-model="aivox.station">
              <option v-for="s in STATION_LIST" :key="s" :value="s">{{ s }}</option>
            </select>
          </div>
          <div class="btn-row">
            <button class="action-btn" @click="aivox.serverAction('POST')">▶ start</button>
            <button class="action-btn danger" @click="aivox.serverAction('DELETE')">■ stop</button>
          </div>
          <div v-if="aivox.cmdStatus" class="filter-label" style="margin-top:4px;opacity:0.7;">{{ aivox.cmdStatus }}</div>
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

      <!-- ── Aivox sidebar ── -->
      <template v-else-if="activeView === 'player'">
        <div class="sidebar-section">
          <span class="section-label">Station</span>
          <div class="select-wrap">
            <select class="station-select" v-model="aivox.station">
              <option v-for="s in STATION_LIST" :key="s" :value="s">{{ s }}</option>
            </select>
          </div>
        </div>
        <div class="sidebar-divider"></div>
        <div class="sidebar-section">
          <div class="stat-item">
            <span class="stat-label">frags loaded</span>
            <span class="stat-value">{{ aivox.fragCount }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">last frag</span>
            <span class="stat-value amber small">{{ aivox.lastFragSize }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">errors</span>
            <span class="stat-value red">{{ aivox.errorCount }}</span>
          </div>
        </div>
        <div class="status-indicator" style="border-top:1px solid var(--border);">
          <div class="status-row">
            <div class="status-dot" :class="aivox.status"></div>
            <span class="status-text">{{ aivox.status }}</span>
          </div>
        </div>
      </template>

      <!-- ── Jesoos sidebar ── -->
      <template v-else-if="activeView === 'jesoos'">
        <div class="sidebar-section">
          <span class="section-label">Brand</span>
          <div class="select-wrap">
            <select class="station-select" v-model="jesoos.brand">
              <option v-for="s in STATION_LIST" :key="s" :value="s">{{ s }}</option>
            </select>
          </div>
        </div>
        <div class="status-indicator" style="border-top:1px solid var(--border);">
          <div class="status-row">
            <div class="status-dot" :class="jesoos.status"></div>
            <span class="status-text">{{ jesoos.status }}</span>
          </div>
        </div>
      </template>

      <!-- ── Traces sidebar ── -->
      <template v-else>
        <div class="filter-section" style="padding-bottom:8px;">
          <div class="filter-label">Filter by brand</div>
          <div class="brand-tabs" style="padding:0;">
            <span v-if="traceBrands.length === 0" class="sidebar-empty">no data yet</span>
            <template v-else>
              <span class="brand-tab" :class="{ active: traces.selectedBrand === 'all' }" @click="traces.selectedBrand = 'all'">all</span>
              <span v-for="brand in traceBrands" :key="brand" class="brand-tab"
                :class="{ active: traces.selectedBrand === brand }" @click="traces.selectedBrand = brand">{{ brand }}</span>
            </template>
          </div>
        </div>
        <div class="sidebar-divider"></div>
        <div class="trace-list">
          <div v-if="tracesForSelectedBrand.length === 0" class="sidebar-empty">no traces</div>
          <div v-for="t in tracesForSelectedBrand" :key="t.id" class="trace-item"
            :class="{ active: traces.selectedTraceId === t.id }" @click="traces.selectedTraceId = t.id">
            <div class="trace-item-id">{{ t.id }}</div>
            <div class="trace-item-meta">
              <span class="trace-count-badge">{{ t.count }}</span>
              <span class="trace-time-label">{{ relTime(t.lastTime) }}</span>
            </div>
          </div>
        </div>
      </template>

      <!-- ── WS status (always visible) ── -->
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
      <span class="topbar-title">{{ topbarTitle }}</span>
      <div class="topbar-right">
        <button v-if="activeView === 'stream'" class="clear-btn" @click="clearAll">CLEAR</button>
        <button v-if="activeView === 'traces'" class="clear-btn"
          :style="traces.showFlowTiming ? 'border-color:var(--accent);color:var(--accent)' : ''"
          @click="traces.showFlowTiming = !traces.showFlowTiming">⏱ TIMING</button>
        <div v-if="activeView === 'player'" class="live-badge" v-show="aivox.status === 'playing'">
          <div class="live-dot"></div><span>LIVE</span>
        </div>
        <div class="live-badge">
          <div class="live-dot" v-show="conn.status === 'connected'"></div>
          <span>{{ conn.status === 'connected' ? 'LIVE' : conn.status.toUpperCase() }}</span>
        </div>
      </div>
    </header>

    <!-- ── Active view ── -->
    <StreamView v-if="activeView === 'stream'"   ref="streamViewRef" />
    <TracesView v-else-if="activeView === 'traces'" />
    <AivoxView  v-else-if="activeView === 'player'" />
    <JesoosView v-else-if="activeView === 'jesoos'" />

  </div>
</template>
