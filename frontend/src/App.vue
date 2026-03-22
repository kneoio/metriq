<script setup lang="ts">
import { computed, onMounted, onUnmounted } from 'vue'
import { useMetriqStore }    from '@/stores/metriq'
import { useConnectionStore } from '@/stores/connection'
import { useAivoxStore }     from '@/stores/aivox'
import { useJesoosStore }    from '@/stores/jesoos'
import { useTracesStore }    from '@/stores/traces'
import { useContextStore }   from '@/stores/context'
import { SERVICE_OPTIONS, STATION_LIST } from '@/utils/service'
import { relTime } from '@/utils/time'
import StreamView   from '@/views/StreamView.vue'
import TracesView   from '@/views/TracesView.vue'
import AivoxView    from '@/views/AivoxView.vue'
import JesoosView   from '@/views/JesoosView.vue'
import GlobalPlayer from '@/components/GlobalPlayer.vue'

// Build info
const appVersion = __APP_VERSION__
const buildTime  = __BUILD_TIME__

// Stores
const metriq   = useMetriqStore()
const conn     = useConnectionStore()
const aivox    = useAivoxStore()
const jesoos   = useJesoosStore()
const traces   = useTracesStore()
const context  = useContextStore()

// Derive the active view from connection store (single source of truth nav)
import { ref, watch } from 'vue'
type View = 'stream' | 'traces' | 'player' | 'jesoos'
const activeView = ref<View>('stream')

// Traces sidebar: always follows the global brand context
const tracesForSelectedBrand = computed(() => {
  const allEntries = Object.entries(metriq.byTrace)
  const filtered = allEntries.filter(([, evts]) =>
    (evts as any[]).some((e: any) => (e.data.brandName ?? '').trim() === context.activeBrand)
  )
  return filtered
    .map(([id, evts]) => ({
      id,
      count: (evts as any[]).length,
      lastTime: (evts as any[])[(evts as any[]).length - 1]?.receivedAt as Date | undefined,
    }))
    .sort((a, b) => (b.lastTime?.getTime() ?? 0) - (a.lastTime?.getTime() ?? 0))
})

// Reset selected trace when brand context changes
watch(() => context.activeBrand, () => { traces.selectedTraceId = null })

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
        <!-- Global view -->
        <div class="nav-item" :class="{ active: activeView === 'stream' }" @click="activeView = 'stream'">
          <span class="nav-dot"></span>All Metrics
        </div>

        <!-- Brand-context views -->
        <div class="nav-section-label">{{ context.activeBrand }}</div>
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
        <div class="status-indicator" style="border-top:1px solid var(--border);">
          <div class="status-row">
            <div class="status-dot" :class="aivox.status"></div>
            <span class="status-text">{{ aivox.status }}</span>
          </div>
        </div>
      </template>

      <!-- ── Jesoos sidebar ── -->
      <template v-else-if="activeView === 'jesoos'">
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
          <div class="filter-label">brand context</div>
          <div class="brand-context-label">{{ context.activeBrand }}</div>
        </div>
        <div class="sidebar-divider"></div>
        <div class="trace-list">
          <div v-if="tracesForSelectedBrand.length === 0" class="sidebar-empty">no traces</div>
          <div v-for="t in tracesForSelectedBrand" :key="t.id" class="trace-item"
            :class="{ active: traces.selectedTraceId === t.id, multi: t.count > 1 }" @click="traces.selectedTraceId = t.id">
            <div class="trace-item-id">{{ t.id }}</div>
            <div class="trace-item-meta">
              <span class="trace-count-badge" :class="{ 'multi-badge': t.count > 1 }">{{ t.count }}</span>
              <span class="trace-time-label">{{ relTime(t.lastTime) }}</span>
            </div>
          </div>
        </div>
      </template>

      <!-- ── Version ── -->
      <div style="padding:4px 12px;font-family:var(--mono);font-size:0.55rem;color:var(--text-muted);opacity:0.5;">
        v{{ appVersion }} · {{ buildTime }}
      </div>

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

      <!-- Left: title + server command controls -->
      <div class="topbar-left">
        <span class="topbar-title">{{ topbarTitle }}</span>
        <div class="aivox-cmd-bar">
          <select class="station-select-mini" v-model="context.activeBrand">
            <option v-for="s in STATION_LIST" :key="s" :value="s">{{ s }}</option>
          </select>
          <button class="action-btn-mini" @click="aivox.serverAction('POST')">▶ Start stream</button>
          <button class="action-btn-mini danger" @click="aivox.serverAction('DELETE')">■ Stop stream</button>
          <span v-if="aivox.cmdStatus" class="cmd-status-mini">{{ aivox.cmdStatus }}</span>
          <div class="topbar-sep"></div>
          <button class="action-btn-mini" @click="jesoos.start()">▶ Start script</button>
          <button class="action-btn-mini danger" @click="jesoos.stop()">■ Stop script</button>
          <button class="action-btn-mini" @click="jesoos.enableDj()">🎙 DJ on</button>
          <button class="action-btn-mini danger" @click="jesoos.disableDj()">🎙 DJ off</button>
          <span v-if="jesoos.cmdStatus" class="cmd-status-mini">{{ jesoos.cmdStatus }}</span>
        </div>
      </div>

      <!-- Right: player + view actions + conn status -->
      <div class="topbar-right">
        <button class="play-btn-mini" :class="{ active: aivox.isPlaying }" @click="aivox.togglePlay()"
          :title="aivox.isPlaying ? 'Pause stream' : 'Play stream'">
          {{ aivox.isPlaying ? '❚❚' : '▶' }}
        </button>
        <div class="now-playing" v-if="aivox.npTitle && aivox.npTitle !== '—'"
          :title="`${aivox.npTitle}${aivox.npArtist && aivox.npArtist !== '—' ? ' · ' + aivox.npArtist : ''}`">
          <span class="np-text">{{ aivox.npTitle }}{{ aivox.npArtist && aivox.npArtist !== '—' ? ' · ' + aivox.npArtist : '' }}</span>
        </div>
        <div class="live-badge" v-show="aivox.isPlaying">
          <div class="live-dot"></div><span>LIVE</span>
        </div>
        <div class="topbar-sep"></div>
        <button v-if="activeView === 'stream'" class="clear-btn" @click="clearAll">CLEAR</button>
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

    <!-- Always mounted — keeps HLS alive regardless of active view -->
    <GlobalPlayer />

  </div>
</template>

<style scoped>
/* ── Topbar left / right split ───────────────────────────────────────────── */
.topbar-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

/* ── Aivox server command bar (left side) ────────────────────────────────── */
.aivox-cmd-bar {
  display: flex;
  align-items: center;
  gap: 6px;
}

.station-select-mini {
  background: var(--surface, #242424);
  color: var(--text, #e0e0e0);
  border: 1px solid var(--border, #333);
  border-radius: 4px;
  font-family: var(--mono, monospace);
  font-size: 0.65rem;
  padding: 2px 4px;
  cursor: pointer;
  max-width: 110px;
}

.action-btn-mini {
  background: transparent;
  color: var(--text, #e0e0e0);
  border: 1px solid var(--border, #444);
  border-radius: 4px;
  font-family: var(--mono, monospace);
  font-size: 0.65rem;
  padding: 2px 7px;
  cursor: pointer;
  white-space: nowrap;
  transition: border-color 0.15s, color 0.15s;
}
.action-btn-mini:hover        { border-color: var(--accent, #2196F3); color: var(--accent, #2196F3); }
.action-btn-mini.danger:hover { border-color: var(--accent3, #fa6d6d); color: var(--accent3, #fa6d6d); }

.play-btn-mini {
  background: transparent;
  color: var(--text, #e0e0e0);
  border: 1px solid var(--border, #444);
  border-radius: 4px;
  font-size: 0.7rem;
  padding: 2px 9px;
  cursor: pointer;
  transition: border-color 0.15s, color 0.15s;
  min-width: 32px;
}
.play-btn-mini:hover  { border-color: var(--accent, #2196F3); color: var(--accent, #2196F3); }
.play-btn-mini.active { border-color: var(--accent, #2196F3); color: var(--accent, #2196F3); }

.cmd-status-mini {
  font-family: var(--mono, monospace);
  font-size: 0.6rem;
  color: var(--text-muted, #666);
  max-width: 90px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.brand-context-label {
  font-family: var(--mono, monospace);
  font-size: 0.65rem;
  color: var(--accent, #2196F3);
  letter-spacing: 0.5px;
  padding: 2px 0;
}

.topbar-sep {
  width: 1px;
  height: 14px;
  background: var(--border, #333);
  margin: 0 2px;
}

.now-playing {
  max-width: 200px;
  overflow: hidden;
}
.np-text {
  font-family: var(--mono, monospace);
  font-size: 0.6rem;
  color: var(--accent, #2196F3);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  display: block;
}
</style>
