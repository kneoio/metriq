<script setup lang="ts">
import { useAivoxStore }    from '@/stores/aivox'
import { useJesoosStore }   from '@/stores/jesoos'
import { useStationsStore } from '@/stores/stations'

const aivox    = useAivoxStore()
const jesoos   = useJesoosStore()
const stations = useStationsStore()
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
          <span class="slabel">{{ jesoos.djEnabled === null ? 'dj ?' : jesoos.djEnabled ? 'dj on' : 'dj off' }}</span>
        </div>
        <div class="dj-led"
          :class="jesoos.liveStatus === null ? 'unknown' : jesoos.liveStatus ? 'connected' : 'disconnected'"
          title="Live status"></div>
        <span class="slabel">{{ jesoos.liveStatus === null ? 'live ?' : jesoos.liveStatus ? 'live' : 'off air' }}</span>
      </div>
      <div v-if="jesoos.liveScene" class="live-scene-info">
        <div class="scene-info-row">
          <span class="scene-label">Scene:</span>
          <span class="scene-value">{{ jesoos.liveScene.sceneTitle }}</span>
        </div>
        <div class="scene-info-row">
          <span class="scene-label">Scheduled:</span>
          <span class="scene-value">{{ jesoos.liveScene.originalStartTime }}</span>
        </div>
        <div v-if="jesoos.liveScene.actualStartTime" class="scene-info-row">
          <span class="scene-label">Started:</span>
          <span class="scene-value">{{ jesoos.liveScene.actualStartTime }}</span>
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

</style>
