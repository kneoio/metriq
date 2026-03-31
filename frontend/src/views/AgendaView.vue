<script setup lang="ts">
import { ref, reactive, computed, nextTick, onMounted, onUnmounted, watch } from 'vue'
import gsap from 'gsap'
import { useContextStore } from '@/stores/context'

const context = useContextStore()

const loading   = ref(false)
const data      = ref<Record<string, any> | null>(null)
const badge     = ref('')
const fetchError = ref<string | null>(null)
const expandedScenes = reactive(new Set<number>())
const statusFilter   = reactive(new Set<string>())

const scenePayloadRefs: Record<number, HTMLElement> = {}

const agendaBrand = computed(() => data.value ? Object.keys(data.value)[0] : '')
const agenda      = computed(() => data.value ? data.value[agendaBrand.value] : null)

const clockNow = ref(Date.now())
let clockInterval: ReturnType<typeof setInterval> | null = null

const stationClock = computed(() => {
  const tz = agenda.value?.timezone
  if (!tz) return null
  try {
    return new Intl.DateTimeFormat('en-GB', {
      timeZone: tz, hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false
    }).format(new Date(clockNow.value))
  } catch { return null }
})

// ── Helpers ───────────────────────────────────────────────────────────────────

function fmtTimeArr(arr: number[] | undefined | null): string {
  if (!arr || arr.length < 5) return '—'
  return `${String(arr[3]).padStart(2, '0')}:${String(arr[4]).padStart(2, '0')}`
}

function fmtDuration(sec: number): string {
  const h = Math.floor(sec / 3600), m = Math.floor((sec % 3600) / 60)
  return h > 0 ? `${h}h ${m}m` : `${m}m`
}

function fmtDurSec(sec: number): string {
  const m = Math.floor(sec / 60), s = sec % 60
  return `${m}:${String(s).padStart(2, '0')}`
}

const STATUS_PRIORITY = ['EMITTING', 'FAILED', 'SCHEDULED', 'PENDING', 'COMPLETED', 'SKIPPED']

function sceneEffectiveStatus(scene: any): string {
  const statuses = new Set((scene.timeline ?? []).map((b: any) => b.status).filter(Boolean))
  return STATUS_PRIORITY.find(s => statuses.has(s)) ?? ''
}


function strategyClass(s: string): string {
  if (s === 'SONG_CROSSFADE_SONG')   return 'strat-xfade'
  if (s === 'INTRO_SONG_INTRO_SONG') return 'strat-isi'
  if (s === 'SONG_INTRO_SONG')       return 'strat-intro'
  if (s === 'INTRO_SONG')            return 'strat-intro'
  if (s === 'FILLER_JINGLE')         return 'strat-jingle'
  return 'strat-only'
}

function sceneEffectiveSongCount(scene: any): number {
  if (scene.totalSongs > 0) return scene.totalSongs
  return (scene.timeline ?? []).reduce((a: number, b: any) => a + (b.songs?.length ?? 0), 0)
}

function sceneEffectiveDuration(scene: any): number {
  if (scene.durationSeconds > 0) return scene.durationSeconds
  return (scene.timeline ?? []).reduce((a: number, b: any) => a + (b.durationSeconds ?? 0), 0)
}

const STATUSES = ['PENDING', 'SCHEDULED', 'EMITTING', 'COMPLETED', 'FAILED', 'SKIPPED'] as const

function toggleStatusFilter(s: string) {
  if (statusFilter.has(s)) statusFilter.delete(s)
  else statusFilter.add(s)
}

function filteredTimeline(timeline: any[]): any[] {
  if (!statusFilter.size) return timeline ?? []
  return (timeline ?? []).filter(b => statusFilter.has(b.status))
}

function statusClass(s: string): string {
  const map: Record<string, string> = {
    PENDING:   'st-pending',
    SCHEDULED: 'st-scheduled',
    EMITTING:  'st-emitting',
    COMPLETED: 'st-completed',
    FAILED:    'st-failed',
    SKIPPED:   'st-skipped',
  }
  return map[s] ?? 'st-pending'
}

// ── Scene expand/collapse ─────────────────────────────────────────────────────

function setSceneRef(idx: number, el: any) {
  if (el) scenePayloadRefs[idx] = el as HTMLElement
  else    delete scenePayloadRefs[idx]
}

function toggleScene(idx: number) {
  const el = scenePayloadRefs[idx]; if (!el) return
  if (expandedScenes.has(idx)) {
    gsap.to(el, { height: 0, duration: 0.3, ease: 'power2.inOut', onComplete() { expandedScenes.delete(idx) } })
  } else {
    expandedScenes.add(idx)
    nextTick(() => {
      const h = el.scrollHeight
      gsap.fromTo(el, { height: 0 }, { height: h, duration: 0.35, ease: 'power2.out', onComplete() { gsap.set(el, { height: 'auto' }) } })
      gsap.fromTo(el.querySelectorAll('.song-row'), { opacity: 0, x: -10 }, { opacity: 1, x: 0, duration: 0.2, stagger: 0.018, ease: 'power2.out' })
    })
  }
}

function copyJson() {
  const payload = JSON.parse(JSON.stringify(data.value))
  const brand = agendaBrand.value
  if (payload?.[brand] && stationClock.value) {
    payload[brand].localTime = stationClock.value
  }
  navigator.clipboard.writeText(JSON.stringify(payload, null, 2))
}

// ── Fetch ─────────────────────────────────────────────────────────────────────

async function fetchAgenda(brand: string) {
  loading.value = true; badge.value = 'pending'; fetchError.value = null
  expandedScenes.clear(); statusFilter.clear()
  try {
    const res  = await fetch(`/jesoos/info/${encodeURIComponent(brand)}/agendas`)
    if (!res.ok) {
      fetchError.value = `${res.status} ${res.statusText}`
      data.value = null; badge.value = 'err'
      return
    }
    const json = await res.json()
    // Response may be { brand: agendaObj } or the agendaObj directly
    if (json && typeof json === 'object' && !Array.isArray(json) && json.scenes == null && Object.keys(json).length > 0) {
      data.value = json
    } else {
      data.value = { [brand]: json }
    }
    badge.value = 'ok'
    nextTick(() => {
      gsap.from('.agenda-header', { opacity: 0, y: -10, duration: 0.3 })
      gsap.from('.scene-card',    { opacity: 0, y: 20,  duration: 0.3, stagger: 0.03, ease: 'power2.out' })
    })
  } catch (e: any) {
    data.value = null; badge.value = 'err'; fetchError.value = e?.message ?? 'fetch failed'
  } finally { loading.value = false }
}

onMounted(() => {
  if (context.activeBrand) fetchAgenda(context.activeBrand)
  clockInterval = setInterval(() => { clockNow.value = Date.now() }, 1000)
})
onUnmounted(() => { if (clockInterval) clearInterval(clockInterval) })
watch(() => context.activeBrand, brand => { if (brand) fetchAgenda(brand) })
</script>

<template>
  <main class="jesoos-main">

    <div class="result-panel">
      <div class="panel-header">
        <span class="panel-title">agendas</span>
        <span v-if="badge" class="panel-badge" :class="badge">
          {{ badge === 'ok' ? 'ok' : badge === 'err' ? 'error' : 'pending' }}
        </span>
        <button class="action-btn secondary" style="margin-left:auto" @click="fetchAgenda(context.activeBrand)">↺ refresh</button>
        <button v-if="data" class="action-btn secondary" @click="copyJson()">⎘ copy json</button>
      </div>
      <div class="panel-body">
        <div v-if="loading" class="loading-text"><span class="spinner"></span>fetching</div>
        <div v-else-if="fetchError" class="fetch-error">
          <span class="fetch-error-brand">{{ context.activeBrand }}</span>
          <span class="fetch-error-msg">{{ fetchError }}</span>
        </div>
        <template v-else-if="data && agenda">

          <!-- ── Agenda header ── -->
          <div class="agenda-header">
            <div class="agenda-stats">
              <div class="stat-box">
                <span class="stat-label">Brand</span>
                <span class="stat-value">{{ agendaBrand }}</span>
              </div>
              <div v-if="agenda.country" class="stat-box">
                <span class="stat-label">Country</span>
                <span class="stat-value">{{ agenda.country }}</span>
              </div>
              <div v-if="agenda.timezone" class="stat-box">
                <span class="stat-label">Timezone</span>
                <span class="stat-value">{{ agenda.timezone }}</span>
              </div>
              <div v-if="stationClock" class="stat-box stat-box-clock">
                <span class="stat-label">Local time</span>
                <span class="stat-value clock-value">{{ stationClock }}</span>
              </div>
              <div class="stat-box">
                <span class="stat-label">Scenes</span>
                <span class="stat-value">{{ agenda.totalScenes }}</span>
              </div>
              <div class="stat-box">
                <span class="stat-label">Duration</span>
                <span class="stat-value">{{ fmtDuration(agenda.scenes.reduce((a: number, s: any) => a + sceneEffectiveDuration(s), 0)) }}</span>
              </div>
              <div class="stat-box">
                <span class="stat-label">Songs</span>
                <span class="stat-value">{{ agenda.scenes.reduce((a: number, s: any) => a + sceneEffectiveSongCount(s), 0) }}</span>
              </div>
            </div>
          </div>

          <!-- ── Status filter ── -->
          <div class="status-filter-bar">
            <button v-for="s in STATUSES" :key="s"
              class="status-filter-btn" :class="[statusClass(s), { active: statusFilter.has(s) }]"
              @click="toggleStatusFilter(s)">
              {{ s.toLowerCase() }}
            </button>
            <button v-if="statusFilter.size" class="status-filter-clear" @click="statusFilter.clear()">clear</button>
          </div>

          <!-- ── Scenes ── -->
          <div class="scenes-list">
            <div class="scene-captions">
              <span>Time window</span>
              <span>Status</span>
              <span>Scene title</span>
              <span>Duration</span>
              <span>Songs</span>
              <span></span>
            </div>
            <template v-for="{scene, idx} in agenda.scenes.map((s: any, i: number) => ({ scene: s, idx: i })).filter(({scene: s}: any) => !statusFilter.size || statusFilter.has(sceneEffectiveStatus(s)))" :key="idx">
            <div class="scene-card" :class="{ expanded: expandedScenes.has(idx) }">

              <div class="scene-row" @click="toggleScene(idx)">
                <span class="scene-time">
                  {{ fmtTimeArr(scene.firstEmissionTime) }}<span class="scene-time-sep">→</span>{{ fmtTimeArr(scene.lastEmissionTime) }}
                </span>
                <span v-if="sceneEffectiveStatus(scene)" class="scene-type-badge" :class="statusClass(sceneEffectiveStatus(scene))">{{ sceneEffectiveStatus(scene).toLowerCase() }}</span>
                <span class="scene-title">{{ scene.title }}</span>
                <span class="scene-duration">{{ fmtDuration(sceneEffectiveDuration(scene)) }}</span>
                <span class="scene-songs" :class="{ 'scene-songs-empty': sceneEffectiveSongCount(scene) === 0 }">
                  {{ sceneEffectiveSongCount(scene) }} songs
                </span>
                <span class="chevron-cell" :class="{ 'chevron-open': expandedScenes.has(idx) }">›</span>
              </div>

              <div class="scene-payload" :class="{ open: expandedScenes.has(idx) }"
                :ref="(el: any) => setSceneRef(idx, el)">
                <div v-if="!scene.timeline?.length" class="timeline-empty">no songs in this scene</div>
                <template v-else>
                  <div class="timeline-bar">
                    <span class="tl-stat">{{ sceneEffectiveSongCount(scene) }} songs</span>
                    <span class="tl-dot">·</span>
                    <span class="tl-stat">{{ fmtDuration(sceneEffectiveDuration(scene)) }}</span>
                    <span v-if="statusFilter.size" class="tl-dot">·</span>
                    <span v-if="statusFilter.size" class="tl-stat tl-filtered">
                      {{ filteredTimeline(scene.timeline).length }} / {{ scene.timeline.length }} shown
                    </span>
                  </div>
                  <div class="timeline-entries">
                    <template v-if="filteredTimeline(scene.timeline).length">
                      <div class="block-captions">
                        <span>#</span>
                        <span>Start time</span>
                        <span>Mixing strategy</span>
                        <span>Extras</span>
                        <span>Duration</span>
                        <span>Status</span>
                      </div>
                      <div v-for="block in filteredTimeline(scene.timeline)" :key="block.id" class="block-item">
                        <div class="block-header">
                          <span class="song-seq">#{{ block.sequenceNumber }}</span>
                          <span class="song-emit-time">{{ fmtTimeArr(block.scheduledEmissionTime) }}</span>
                          <span class="song-strategy" :class="strategyClass(block.mixingStrategy)">{{ block.mixingStrategy }}</span>
                          <span class="song-flags">
                            <span v-if="block.hasIntro"  class="flag flag-intro">I</span>
                            <span v-if="block.hasJingle" class="flag flag-jingle">J</span>
                          </span>
                          <span class="song-dur">{{ fmtDurSec(block.durationSeconds) }}</span>
                          <span class="entry-status" :class="statusClass(block.status)">{{ (block.status ?? '').toLowerCase() }}</span>
                        </div>
                        <div v-for="song in block.songs" :key="song.songId" class="song-row">
                          <div class="song-info">
                            <span class="song-title">{{ song.songTitle }}</span>
                            <span class="song-artist">{{ song.artist }}</span>
                          </div>
                          <span class="song-dur">{{ fmtDurSec(song.durationSeconds) }}</span>
                        </div>
                      </div>
                    </template>
                    <div v-else class="timeline-empty">no entries match filter</div>
                  </div>
                </template>
              </div>
            </div>
            </template>
          </div>

        </template>
        <div v-else class="panel-empty">no agenda for {{ context.activeBrand || '—' }}</div>
      </div>
    </div>
  </main>
</template>

<style scoped>
.clock-value { font-family: var(--mono); letter-spacing: 1px; color: var(--accent2); }

.scene-time { font-family: var(--mono); font-size: 0.68rem; font-weight: 600; letter-spacing: 0.5px; color: var(--accent2); white-space: nowrap; }
.scene-time-sep { color: var(--text-dim); margin: 0 4px; font-weight: 400; }
.scene-row { grid-template-columns: max-content max-content 1fr max-content max-content auto; }
.scene-songs-empty { color: var(--text-dim); border-color: rgba(255,255,255,0.06); }
.chevron-cell { transition: transform 0.25s; display: inline-block; }
.chevron-open { transform: rotate(90deg); }

.timeline-bar {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 16px;
  background: rgba(0,0,0,0.2);
  border-bottom: 1px solid var(--border);
  font-family: var(--mono); font-size: 0.62rem; letter-spacing: 0.5px;
}
.tl-stat     { color: var(--text-dim); }
.tl-dot      { color: var(--border-bright); }
.tl-filtered { color: var(--amber); }

.status-filter-bar {
  display: flex; align-items: center; gap: 6px; flex-wrap: wrap;
  padding: 8px 16px; border-bottom: 1px solid var(--border);
}
.status-filter-btn {
  font-family: var(--mono); font-size: 0.58rem; letter-spacing: 0.5px;
  padding: 2px 8px; border-radius: 3px; border: 1px solid; cursor: pointer;
  background: transparent; transition: background 0.15s, opacity 0.15s;
  opacity: 0.5;
}
.status-filter-btn.active { opacity: 1; }
.status-filter-clear {
  font-family: var(--mono); font-size: 0.58rem; letter-spacing: 0.5px;
  padding: 2px 8px; border-radius: 3px; border: 1px solid var(--border);
  color: var(--text-dim); background: transparent; cursor: pointer; margin-left: 4px;
}
.status-filter-clear:hover { border-color: var(--border-bright); color: var(--text); }

.timeline-entries { padding: 12px 16px; display: flex; flex-direction: column; gap: 6px; }

.block-captions {
  display: grid;
  grid-template-columns: 28px 44px 1fr 32px 48px max-content;
  align-items: center; gap: 8px;
  padding: 2px 6px 6px;
  font-family: var(--mono); font-size: 0.52rem; letter-spacing: 1px;
  color: var(--text-dim); text-transform: uppercase;
  border-bottom: 1px solid var(--border); margin-bottom: 4px;
}

.scene-captions {
  display: grid;
  grid-template-columns: max-content max-content 1fr max-content max-content auto;
  align-items: center; gap: 8px;
  padding: 4px 16px 6px;
  font-family: var(--mono); font-size: 0.52rem; letter-spacing: 1px;
  color: var(--text-dim); text-transform: uppercase;
  border-bottom: 1px solid var(--border); margin-bottom: 4px;
}

.block-item {
  display: flex; flex-direction: column; gap: 1px;
  border-left: 2px solid var(--border-bright); padding-left: 8px;
}
.block-header {
  display: grid;
  grid-template-columns: 28px 44px 1fr 32px 48px max-content;
  align-items: center; gap: 8px;
  padding: 4px 6px; border-radius: 3px;
  background: rgba(255,255,255,0.03);
}
.entry-status {
  font-family: var(--mono); font-size: 0.56rem; letter-spacing: 0.5px;
  padding: 2px 6px; border-radius: 3px; border: 1px solid; text-align: center; white-space: nowrap;
}
.song-row {
  display: grid; grid-template-columns: 1fr 42px;
  align-items: center; gap: 8px;
  padding: 4px 6px 4px 12px; border-radius: 3px;
  background: rgba(255,255,255,0.015); border: 1px solid transparent;
  transition: border-color 0.15s, background 0.15s;
}
.song-row:hover { background: rgba(255,255,255,0.035); border-color: var(--border); }

.song-seq       { font-family: var(--mono); font-size: 0.6rem; color: var(--text-dim); text-align: right; }
.song-emit-time { font-family: var(--mono); font-size: 0.62rem; color: var(--accent2); letter-spacing: 0.5px; }
.song-info      { display: flex; flex-direction: column; gap: 1px; overflow: hidden; }
.song-title     { font-size: 0.8rem; color: var(--text); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.song-artist    { font-size: 0.65rem; color: var(--text-dim); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.song-dur       { font-family: var(--mono); font-size: 0.62rem; color: var(--text-muted); text-align: right; }

.song-strategy {
  font-family: var(--mono); font-size: 0.58rem; letter-spacing: 0.5px;
  padding: 2px 6px; border-radius: 3px; border: 1px solid; text-align: center; white-space: nowrap;
}
.strat-only   { color: var(--text-dim);  border-color: rgba(255,255,255,0.1); background: transparent; }
.strat-intro  { color: var(--accent);    border-color: rgba(33,150,243,0.3);  background: rgba(33,150,243,0.06); }
.strat-isi    { color: var(--purple);    border-color: rgba(176,132,255,0.3); background: rgba(176,132,255,0.06); }
.strat-xfade  { color: var(--amber);     border-color: rgba(245,166,35,0.3);  background: rgba(245,166,35,0.06); }
.strat-jingle { color: var(--green);     border-color: rgba(76,175,80,0.3);   background: rgba(76,175,80,0.06); }

.song-flags { display: flex; gap: 3px; }
.flag { font-family: var(--mono); font-size: 0.58rem; font-weight: 600; width: 16px; height: 16px; border-radius: 3px; display: flex; align-items: center; justify-content: center; }
.flag-intro  { background: rgba(33,150,243,0.2); color: var(--accent2); }
.flag-jingle { background: rgba(245,166,35,0.2);  color: var(--amber); }

.st-pending   { color: var(--text-dim);  border-color: rgba(255,255,255,0.12); }
.st-scheduled { color: var(--accent);    border-color: rgba(33,150,243,0.35);  background: rgba(33,150,243,0.06); }
.st-emitting  { color: var(--green);     border-color: rgba(76,175,80,0.4);    background: rgba(76,175,80,0.08); }
.st-completed { color: var(--green);      border-color: rgba(76,175,80,0.3);    background: rgba(76,175,80,0.05); opacity: 0.7; }
.st-failed    { color: var(--red,#f44);  border-color: rgba(244,67,54,0.4);    background: rgba(244,67,54,0.07); }
.st-skipped   { color: var(--amber);     border-color: rgba(245,166,35,0.25);  background: rgba(245,166,35,0.04); opacity: 0.7; }

.fetch-error {
  display: flex; flex-direction: column; gap: 6px;
  padding: 24px 20px;
}
.fetch-error-brand {
  font-size: 1rem; font-weight: 600; color: var(--text); letter-spacing: 0.5px;
}
.fetch-error-msg {
  font-family: var(--mono); font-size: 0.65rem; letter-spacing: 0.5px;
  color: var(--red, #f44336);
}

.timeline-empty {
  padding: 14px 16px;
  font-family: var(--mono); font-size: 0.65rem; letter-spacing: 1px;
  color: var(--text-dim); text-transform: uppercase;
}
</style>
