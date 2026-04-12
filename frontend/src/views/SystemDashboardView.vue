<script setup lang="ts">
import { ref, onMounted } from 'vue'

interface CleanupStats {
  filesDeleted: number
  bytesFreed:   number
  mbFreed:      number
  lastCleanupTime: string | null
  folders: string[]
}

interface SoundFragmentCleanupStats {
  expiredFragmentsDeleted: number
  archivedFragmentsDeleted: number
  totalFragmentsDeleted: number
  lastCleanupTime: string | null
}

const cleanupStats  = ref<CleanupStats | null>(null)
const cleanupError  = ref<string | null>(null)
const cleanupLoading = ref(false)

const soundFragmentStats  = ref<SoundFragmentCleanupStats | null>(null)
const soundFragmentError  = ref<string | null>(null)
const soundFragmentLoading = ref(false)

async function fetchCleanupStats() {
  cleanupLoading.value = true
  cleanupError.value   = null
  try {
    const res = await fetch('/metriq/cleanup/stats')
    if (!res.ok) { cleanupError.value = `${res.status} ${res.statusText}`; return }
    cleanupStats.value = await res.json()
  } catch (e: any) {
    cleanupError.value = e?.message ?? 'fetch failed'
  } finally {
    cleanupLoading.value = false
  }
}

async function fetchSoundFragmentStats() {
  soundFragmentLoading.value = true
  soundFragmentError.value   = null
  try {
    const res = await fetch('/metriq/cleanup/soundfragment/stats')
    if (!res.ok) { soundFragmentError.value = `${res.status} ${res.statusText}`; return }
    soundFragmentStats.value = await res.json()
  } catch (e: any) {
    soundFragmentError.value = e?.message ?? 'fetch failed'
  } finally {
    soundFragmentLoading.value = false
  }
}

onMounted(() => {
  fetchCleanupStats()
  fetchSoundFragmentStats()
})
</script>

<template>
  <main class="sysdash-main">
    <div class="sysdash-grid">

      <!-- ── File Cleanup card ── -->
      <div class="dash-card">
        <div class="dash-card-header">
          <span class="dash-card-title">File Cleanup</span>
          <button class="action-btn secondary" @click="fetchCleanupStats">↺ refresh</button>
        </div>
        <div class="dash-card-body">
          <div v-if="cleanupLoading" class="loading-text"><span class="spinner"></span>fetching</div>
          <div v-else-if="cleanupError" class="card-error">{{ cleanupError }}</div>
          <template v-else-if="cleanupStats">
            <div class="stat-row">
              <span class="stat-label">Files deleted</span>
              <span class="stat-value">{{ cleanupStats.filesDeleted }}</span>
            </div>
            <div class="stat-row">
              <span class="stat-label">Space freed</span>
              <span class="stat-value">{{ cleanupStats.mbFreed }} MB</span>
            </div>
            <div class="stat-row">
              <span class="stat-label">Bytes freed</span>
              <span class="stat-value mono">{{ cleanupStats.bytesFreed.toLocaleString() }}</span>
            </div>
            <div class="stat-row">
              <span class="stat-label">Last run</span>
              <span class="stat-value mono">{{ cleanupStats.lastCleanupTime ? cleanupStats.lastCleanupTime.split('.')[0] : '—' }}</span>
            </div>
            <div class="stat-row stat-row-col" v-if="cleanupStats.folders.length">
              <span class="stat-label">Watched folders</span>
              <div class="folder-list">
                <span v-for="f in cleanupStats.folders" :key="f" class="folder-item">{{ f }}</span>
              </div>
            </div>
          </template>
          <div v-else class="card-empty">no data</div>
        </div>
      </div>

      <!-- ── SoundFragment Cleanup card ── -->
      <div class="dash-card">
        <div class="dash-card-header">
          <span class="dash-card-title">SoundFragment Cleanup</span>
          <button class="action-btn secondary" @click="fetchSoundFragmentStats">↺ refresh</button>
        </div>
        <div class="dash-card-body">
          <div v-if="soundFragmentLoading" class="loading-text"><span class="spinner"></span>fetching</div>
          <div v-else-if="soundFragmentError" class="card-error">{{ soundFragmentError }}</div>
          <template v-else-if="soundFragmentStats">
            <div class="stat-row">
              <span class="stat-label">Total deleted</span>
              <span class="stat-value">{{ soundFragmentStats.totalFragmentsDeleted }}</span>
            </div>
            <div class="stat-row">
              <span class="stat-label">Expired deleted</span>
              <span class="stat-value">{{ soundFragmentStats.expiredFragmentsDeleted }}</span>
            </div>
            <div class="stat-row">
              <span class="stat-label">Archived deleted</span>
              <span class="stat-value">{{ soundFragmentStats.archivedFragmentsDeleted }}</span>
            </div>
            <div class="stat-row">
              <span class="stat-label">Last run</span>
              <span class="stat-value mono">{{ soundFragmentStats.lastCleanupTime ? soundFragmentStats.lastCleanupTime.split('.')[0] : '—' }}</span>
            </div>
          </template>
          <div v-else class="card-empty">no data</div>
        </div>
      </div>

    </div>
  </main>
</template>

<style scoped>
.sysdash-main {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  background: var(--bg);
}

.sysdash-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
  align-items: start;
}

.dash-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 6px;
  overflow: hidden;
}

.dash-card-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  border-bottom: 1px solid var(--border);
  background: rgba(255,255,255,0.02);
}

.dash-card-title {
  font-family: var(--mono);
  font-size: 0.65rem;
  letter-spacing: 1px;
  text-transform: uppercase;
  color: var(--text-muted);
  flex: 1;
}

.dash-card-body {
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.stat-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  padding: 4px 0;
  border-bottom: 1px solid rgba(255,255,255,0.04);
}
.stat-row:last-child { border-bottom: none; }
.stat-row-col { flex-direction: column; align-items: flex-start; gap: 6px; }

.stat-label {
  font-family: var(--mono);
  font-size: 0.6rem;
  letter-spacing: 0.5px;
  color: var(--text-dim);
  text-transform: uppercase;
  white-space: nowrap;
}

.stat-value {
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--text);
}
.stat-value.mono {
  font-family: var(--mono);
  font-size: 0.7rem;
  font-weight: 400;
  color: var(--text-muted);
}

.folder-list {
  display: flex;
  flex-direction: column;
  gap: 3px;
  width: 100%;
}
.folder-item {
  font-family: var(--mono);
  font-size: 0.6rem;
  color: var(--text-dim);
  background: rgba(255,255,255,0.03);
  border: 1px solid var(--border);
  border-radius: 3px;
  padding: 3px 8px;
  word-break: break-all;
}

.card-error {
  font-family: var(--mono);
  font-size: 0.65rem;
  color: var(--red, #f44336);
}
.card-empty {
  font-family: var(--mono);
  font-size: 0.65rem;
  color: var(--text-dim);
}

.action-btn {
  font-family: var(--mono);
  font-size: 0.65rem;
  padding: 4px 10px;
  border: 1px solid var(--border);
  border-radius: 4px;
  background: rgba(255,255,255,0.05);
  color: var(--text-muted);
  cursor: pointer;
  transition: all 0.15s;
}
.action-btn:hover {
  background: rgba(255,255,255,0.1);
  color: var(--text);
}

.loading-text {
  font-family: var(--mono);
  font-size: 0.65rem;
  color: var(--text-dim);
  display: flex;
  align-items: center;
  gap: 8px;
}

.spinner {
  display: inline-block;
  width: 12px;
  height: 12px;
  border: 2px solid rgba(255,255,255,0.2);
  border-top-color: var(--text-muted);
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
