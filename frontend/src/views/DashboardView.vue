<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useAivoxStore }    from '@/stores/aivox'
import { useJesoosStore }   from '@/stores/jesoos'
import { useStationsStore } from '@/stores/stations'

const aivox    = useAivoxStore()
const jesoos   = useJesoosStore()
const stations = useStationsStore()

// ── Playlist ──────────────────────────────────────────────────────────────────

interface PlaylistEntry {
  songId:         string
  title:          string
  artist:         string
  duration:       number
  status:         'playing' | 'played' | 'queued'
  queue?:         'priority' | 'regular'
  mergingMethod?: string | null
  receivedAt?:    number
}

function schematic(method: string | null | undefined): string {
  if (!method) return ''
  switch (method) {
    case 'SONG_CROSSFADE_SONG':       return '≋ crossfade'
    case 'SONG_INTRO_SONG':           return '▸ intro → song'
    case 'INTRO_SONG_INTRO_SONG':     return '▸ intro → song → intro → song'
    case 'SONG_ONLY':                 return '▸ song only'
    case 'FILLER_JINGLE':             return '♪ jingle'
    case 'SONG_CROSSFADE_INTRO_SONG': return '≋ crossfade → intro → song'
    default: return method.toLowerCase().replace(/_/g, ' ')
  }
}

const playlist = ref<PlaylistEntry[]>([])
let ws: WebSocket | null = null

async function fetchPlaylist(brand: string) {
  try {
    const res = await fetch(`/metriq/playlist/${encodeURIComponent(brand)}`)
    if (res.ok) {
      const data = await res.json()
      playlist.value = Array.isArray(data) ? data : [data]
    }
  } catch (e) {
    console.error('[dashboard] playlist fetch failed', e)
  }
}

function applyQueueUpdate(payload: any) {
  playlist.value = playlist.value.filter(e => e.status !== 'queued')
  const prio: any[] = payload.prioritizedQueueSongs ?? []
  const reg:  any[] = payload.regularQueueSongs     ?? []
  prio.forEach(s => playlist.value.push({ songId: s.songId ?? '', title: s.title ?? '', artist: s.artist ?? '', duration: s.duration ?? 0, status: 'queued', queue: 'priority', mergingMethod: s.mergingMethod ?? null }))
  reg.forEach(s  => playlist.value.push({ songId: s.songId ?? '', title: s.title ?? '', artist: s.artist ?? '', duration: s.duration ?? 0, status: 'queued', queue: 'regular',  mergingMethod: s.mergingMethod ?? null }))
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
        playlist.value.forEach(e => { if (e.status === 'playing') e.status = 'played' })
        playlist.value = playlist.value.filter(e => e.status !== 'queued')
        playlist.value.push({ songId: event.payload.songId ?? '', title: event.payload.title ?? '', artist: event.payload.artist ?? '', duration: event.payload.duration ?? 0, status: 'playing', receivedAt: event._receivedAt ?? Date.now() })
      } else if (code === 'song_ended') {
        playlist.value.forEach(e => { if (e.status === 'playing') e.status = 'played' })
      } else if (code === 'queue_updated' && event.payload) {
        applyQueueUpdate(event.payload)
      }
    } catch (e) { console.error('[dashboard ws] parse error', e) }
  }
  ws.onclose = () => { setTimeout(() => connectWs(brand), 3000) }
}

function initPlaylist(brand: string) {
  playlist.value = []
  fetchPlaylist(brand)
  connectWs(brand)
}

onMounted(() => initPlaylist(stations.activeStation))
watch(() => stations.activeStation, initPlaylist)
onUnmounted(() => { if (ws) { ws.onclose = null; ws.close(); ws = null } })
</script>

<template>
  <main class="dashboard-main">

    <div class="dash-station-name">{{ stations.activeStation }}</div>

    <!-- ── Aivox ── -->
    <div class="dash-section">
      <div class="dash-section-title">AIVOX</div>
      <div class="dash-controls">
        <button class="dash-btn"        @click="aivox.serverAction('POST')">▶ Start stream</button>
        <button class="dash-btn danger" @click="aivox.serverAction('DELETE')">■ Stop stream</button>
      </div>
      <div class="dash-feedback">
        <div class="dj-led"
          :class="aivox.heartbeat === null ? 'unknown' : aivox.heartbeat ? 'connected' : 'disconnected'"
          title="Aivox heartbeat"></div>
        <span class="slabel">{{ aivox.heartbeat === null ? '?' : aivox.heartbeat ? 'hls serving' : 'dead' }}</span>
        <span v-if="aivox.cmdStatus" class="dash-cmd-status">{{ aivox.cmdStatus }}</span>
      </div>
    </div>

    <!-- ── Jesoos ── -->
    <div class="dash-section">
      <div class="dash-section-title">JESOOS</div>
      <div class="dash-controls">
        <button class="dash-btn"        @click="jesoos.start()">▶ Start script</button>
        <button class="dash-btn danger" @click="jesoos.stop()">■ Stop script</button>
        <button class="dash-btn"        @click="jesoos.enableDj()">🎙 DJ on</button>
        <button class="dash-btn danger" @click="jesoos.disableDj()">🎙 DJ off</button>
      </div>
      <div class="dash-feedback">
        <div class="dash-svc-status">
          <span v-if="jesoos.cmdStatus" class="slabel">{{ jesoos.cmdStatus }}</span>
          <div class="dj-led"
            :class="jesoos.djEnabled === null ? 'unknown' : jesoos.djEnabled ? 'connected' : 'disconnected'"
            title="DJ status"></div>
          <span class="slabel">{{ jesoos.djEnabled === null ? 'dj ?' : jesoos.djEnabled ? 'dj is on' : 'dj off' }}</span>
        </div>
        <div class="dj-led"
          :class="jesoos.liveStatus === null ? 'unknown' : jesoos.liveStatus ? 'connected' : 'disconnected'"
          title="Live status"></div>
        <span class="slabel">{{ jesoos.liveStatus === null ? 'live ?' : jesoos.liveStatus && jesoos.liveScene ? 'current scene: ' + jesoos.liveScene.sceneTitle : jesoos.liveStatus ? 'live' : 'off air' }}</span>
      </div>
      <div v-if="jesoos.liveScene" class="live-scene-info">
        <pre class="scene-raw">{{ JSON.stringify(jesoos.liveScene.raw, null, 2) }}</pre>
      </div>
    </div>

    <!-- ── Playlist ── -->
    <div class="dash-section">
      <div class="dash-section-title">PLAYLIST</div>
      <div v-if="playlist.length === 0" class="pl-empty">no playlist data yet</div>
      <div v-else class="pl-list">
        <div v-for="(entry, idx) in playlist" :key="entry.songId + idx" class="pl-item" :class="entry.status">
          <div class="pl-dot" :class="entry.status"></div>
          <div class="pl-info">
            <span class="pl-title">{{ entry.title }}</span>
            <span class="pl-artist">{{ entry.artist }}</span>
            <span v-if="entry.mergingMethod" class="pl-method">{{ schematic(entry.mergingMethod) }}</span>
          </div>
          <span v-if="entry.queue === 'priority'" class="pl-qmark priority" title="priority queue">★</span>
          <span v-else-if="entry.queue === 'regular'" class="pl-qmark regular" title="regular queue">○</span>
          <div class="pl-badge" :class="entry.status">{{ entry.status }}</div>
        </div>
      </div>
    </div>

  </main>
</template>

<style scoped>
.dashboard-main {
  flex: 1;
  padding: 36px 44px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 28px;
}

.dash-station-name {
  font-family: var(--mono);
  font-size: 1.1rem;
  font-weight: 700;
  letter-spacing: 3px;
  color: var(--accent);
  text-transform: uppercase;
  margin-bottom: 4px;
}

.dash-section {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 22px 26px;
  border: 1px solid var(--border);
  border-radius: 6px;
  background: var(--surface);
  max-width: 640px;
}

.dash-section-title {
  font-family: var(--mono);
  font-size: 0.6rem;
  letter-spacing: 2.5px;
  color: var(--text-muted);
}

.dash-controls {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.dash-btn {
  background: transparent;
  color: var(--text);
  border: 1px solid var(--border-bright);
  border-radius: 4px;
  font-family: var(--mono);
  font-size: 0.78rem;
  padding: 9px 20px;
  cursor: pointer;
  white-space: nowrap;
  transition: border-color 0.15s, color 0.15s;
}
.dash-btn:hover        { border-color: var(--accent);  color: var(--accent); }
.dash-btn.danger:hover { border-color: var(--accent3); color: var(--accent3); }

.dash-feedback {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.dash-svc-status {
  display: flex;
  align-items: center;
  gap: 6px;
}

.sdot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--text-dim);
  flex-shrink: 0;
  transition: background 0.3s;
}
.sdot.running, .sdot.connected { background: var(--green); }
.sdot.error                    { background: var(--accent3); }

.slabel {
  font-family: var(--mono);
  font-size: 0.62rem;
  color: var(--text-muted);
}

.dash-cmd-status {
  font-family: var(--mono);
  font-size: 0.62rem;
  color: var(--text-muted);
}

.dj-led {
  width: 8px; height: 8px; border-radius: 50%;
  background: var(--text-dim); flex-shrink: 0;
  transition: background 0.3s; margin-left: 6px;
}
.dj-led.connected    { background: var(--green); box-shadow: 0 0 8px var(--green); animation: pulse-dot 2s ease-in-out infinite; }
.dj-led.disconnected { background: var(--accent3); }
.dj-led.unknown      { background: var(--text-dim); }

.live-scene-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 8px;
  padding: 14px 16px;
  border: 1px solid var(--border);
  border-radius: 4px;
  background: rgba(0, 0, 0, 0.2);
}

.scene-info-row {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.scene-label {
  font-family: var(--mono);
  font-size: 0.58rem;
  letter-spacing: 1px;
  color: var(--text-dim);
  text-transform: uppercase;
  min-width: 80px;
}

.scene-value {
  font-family: var(--mono);
  font-size: 0.72rem;
  color: var(--accent2);
  font-weight: 500;
}

.scene-raw {
  font-family: var(--mono);
  font-size: 0.55rem;
  line-height: 1.5;
  color: var(--text-muted);
  white-space: pre-wrap;
  word-break: break-all;
  margin-top: 8px;
  padding: 10px 12px;
  background: rgba(0,0,0,0.25);
  border: 1px solid var(--border);
  border-radius: 4px;
  max-height: 300px;
  overflow-y: auto;
}

/* ── Playlist card ── */
.pl-empty {
  font-family: var(--mono);
  font-size: 0.65rem;
  color: var(--text-dim);
}

.pl-list {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.pl-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border-radius: 4px;
  background: rgba(255,255,255,0.02);
  border: 1px solid var(--border);
  transition: opacity 0.2s;
}

.pl-item.played  { opacity: 0.4; }
.pl-item.playing { border-color: var(--accent); background: rgba(33,150,243,0.06); }
.pl-item.queued  { opacity: 0.65; }

.pl-dot {
  width: 7px; height: 7px; border-radius: 50%; flex-shrink: 0;
}
.pl-dot.playing { background: var(--accent); box-shadow: 0 0 5px var(--accent); animation: pulse-dot 1.4s ease-in-out infinite; }
.pl-dot.played  { background: var(--text-dim); }
.pl-dot.queued  { background: transparent; border: 1px solid var(--text-dim); }

.pl-info {
  display: flex;
  flex-direction: column;
  gap: 1px;
  flex: 1;
  min-width: 0;
}

.pl-title  { font-family: var(--mono); font-size: 0.75rem; color: var(--text);       white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.pl-artist { font-family: var(--mono); font-size: 0.6rem;  color: var(--text-muted); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.pl-method { font-family: var(--mono); font-size: 0.55rem; color: var(--cyan, #26c6da); letter-spacing: 0.5px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

.pl-qmark          { font-size: 0.6rem; flex-shrink: 0; }
.pl-qmark.priority { color: var(--amber); }
.pl-qmark.regular  { color: var(--text-dim); }

.pl-badge {
  font-family: var(--mono);
  font-size: 0.5rem;
  letter-spacing: 1px;
  text-transform: uppercase;
  padding: 2px 5px;
  border-radius: 3px;
  flex-shrink: 0;
}
.pl-badge.playing { background: rgba(33,150,243,0.15); color: var(--accent); }
.pl-badge.played  { background: rgba(255,255,255,0.04); color: var(--text-dim); }
.pl-badge.queued  { background: rgba(255,255,255,0.03); color: var(--text-dim); }
</style>
