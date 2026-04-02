<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useStationsStore } from '@/stores/stations'

interface PlaylistEntry {
  songId:     string
  title:      string
  artist:     string
  duration:   number
  status:     'playing' | 'played'
  receivedAt: number
}

const stations = useStationsStore()
const playlist  = ref<PlaylistEntry[]>([])

let ws: WebSocket | null = null

async function fetchPlaylist(brand: string) {
  try {
    const res = await fetch(`/metriq/playlist/${encodeURIComponent(brand)}`)
    if (res.ok) {
      const data = await res.json()
      playlist.value = Array.isArray(data) ? data : [data]
    }
  } catch (e) {
    console.error('[user-dashboard] fetch failed', e)
  }
}

function connectWs(brand: string) {
  if (ws) { ws.onclose = null; ws.close(); ws = null }
  const scheme = window.location.protocol === 'https:' ? 'wss' : 'ws'
  ws = new WebSocket(`${scheme}://${window.location.host}/metriq/ws/metrics/${encodeURIComponent(brand)}`)

  ws.onmessage = (msg: MessageEvent) => {
    try {
      const event = JSON.parse(msg.data as string)
      const code  = event.code as string | undefined

      if (code === 'now_playing' && event.payload) {
        // Mark current playing as played
        playlist.value.forEach(e => { if (e.status === 'playing') e.status = 'played' })
        playlist.value.push({
          songId:     event.payload.songId   ?? '',
          title:      event.payload.title    ?? '',
          artist:     event.payload.artist   ?? '',
          duration:   event.payload.duration ?? 0,
          status:     'playing',
          receivedAt: event._receivedAt ?? Date.now(),
        })
      } else if (code === 'song_ended') {
        playlist.value.forEach(e => { if (e.status === 'playing') e.status = 'played' })
      }
    } catch (e) {
      console.error('[user-dashboard ws] parse error', e)
    }
  }

  ws.onclose = () => {
    setTimeout(() => connectWs(brand), 3000)
  }
}

function init(brand: string) {
  fetchPlaylist(brand)
  connectWs(brand)
}

onMounted(() => init(stations.activeStation))

watch(() => stations.activeStation, (brand) => {
  playlist.value = []
  init(brand)
})

onUnmounted(() => {
  if (ws) { ws.onclose = null; ws.close(); ws = null }
})
</script>

<template>
  <main class="ud-main">
    <div class="ud-empty" v-if="playlist.length === 0">no playlist data yet</div>
    <div class="ud-list" v-else>
      <div
        v-for="(entry, idx) in [...playlist].reverse()"
        :key="entry.songId + idx"
        class="ud-item"
        :class="entry.status"
      >
        <div class="ud-status-indicator" :class="entry.status"></div>
        <div class="ud-song-info">
          <span class="ud-title">{{ entry.title }}</span>
          <span class="ud-artist">{{ entry.artist }}</span>
        </div>
        <div class="ud-badge" :class="entry.status">{{ entry.status }}</div>
      </div>
    </div>
  </main>
</template>

<style scoped>
.ud-main {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  font-family: var(--mono);
}

.ud-empty {
  color: var(--text-muted);
  font-size: 0.75rem;
  padding: 20px 0;
}

.ud-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.ud-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  border-radius: 4px;
  background: rgba(255,255,255,0.03);
  border: 1px solid var(--border, #333);
  transition: opacity 0.2s;
}

.ud-item.played {
  opacity: 0.45;
}

.ud-item.playing {
  border-color: var(--accent, #4fc3f7);
  background: rgba(79,195,247,0.05);
}

.ud-status-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.ud-status-indicator.playing {
  background: var(--accent, #4fc3f7);
  box-shadow: 0 0 6px var(--accent, #4fc3f7);
  animation: pulse 1.4s ease-in-out infinite;
}

.ud-status-indicator.played {
  background: var(--text-muted, #555);
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50%       { opacity: 0.4; }
}

.ud-song-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
  min-width: 0;
}

.ud-title {
  font-size: 0.8rem;
  color: var(--text, #eee);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ud-artist {
  font-size: 0.65rem;
  color: var(--text-muted, #777);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ud-badge {
  font-size: 0.55rem;
  letter-spacing: 1px;
  text-transform: uppercase;
  padding: 2px 6px;
  border-radius: 3px;
  flex-shrink: 0;
}

.ud-badge.playing {
  background: rgba(79,195,247,0.15);
  color: var(--accent, #4fc3f7);
}

.ud-badge.played {
  background: rgba(255,255,255,0.05);
  color: var(--text-muted, #555);
}
</style>
