<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useMetriqStore } from '@/stores/metriq'
import { useConnectionStore } from '@/stores/connection'
import StreamView from '@/views/StreamView.vue'
import TracesView from '@/views/TracesView.vue'
import AivoxView  from '@/views/AivoxView.vue'
import JesoosView from '@/views/JesoosView.vue'

type View = 'stream' | 'traces' | 'player' | 'jesoos'

const store      = useMetriqStore()
const conn       = useConnectionStore()
const activeView = ref<View>('stream')

const topbarTitle = computed(() => {
  const map: Record<View, string> = {
    stream: 'ALL METRICS',
    traces: 'TRACE FLOW',
    player: 'AIVOX CONTROL',
    jesoos: 'JESOOS CONTROL',
  }
  return map[activeView.value]
})

// Start WebSocket on mount
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

      <!-- Active view teleports its sidebar content here -->
      <div id="view-sidebar" style="flex:1; display:flex; flex-direction:column; overflow-y:auto; min-height:0;"></div>

      <!-- WS status (always visible) -->
      <div class="status-indicator">
        <div class="status-row">
          <div class="status-dot" :class="conn.status"></div>
          <span class="status-text">{{ conn.status }}</span>
        </div>
        <div class="host-row">
          <input class="host-input" v-model="conn.wsHost" @keydown.enter="conn.reconnect()" placeholder="host:port" spellcheck="false" />
          <button class="reconnect-btn" @click="conn.reconnect()" title="Reconnect">↺</button>
        </div>
      </div>
    </aside>

    <!-- ── Topbar ── -->
    <header class="topbar">
      <span class="topbar-title">{{ topbarTitle }}</span>
      <div class="topbar-right">
        <!-- Stream: clear button -->
        <button v-if="activeView === 'stream'" class="clear-btn" @click="(store as any).clearAllEvents?.()">CLEAR</button>
        <!-- Other views inject their own topbar actions here -->
        <div id="topbar-action"></div>
        <!-- WS live badge -->
        <div class="live-badge">
          <div class="live-dot" v-show="conn.status === 'connected'"></div>
          <span>{{ conn.status === 'connected' ? 'LIVE' : conn.status.toUpperCase() }}</span>
        </div>
      </div>
    </header>

    <!-- ── Active view ── -->
    <StreamView v-if="activeView === 'stream'" />
    <TracesView v-else-if="activeView === 'traces'" />
    <AivoxView  v-else-if="activeView === 'player'" />
    <JesoosView v-else-if="activeView === 'jesoos'" />
  </div>
</template>
