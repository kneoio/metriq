<script setup lang="ts">
import { ref, reactive, computed, nextTick } from 'vue'
import gsap from 'gsap'

// ── Local state ───────────────────────────────────────────────────────────────
const jesoosAgendasLoading   = ref(false)
const jesoosAgendasData      = ref<Record<string, any> | null>(null)
const jesoosAgendasBadge     = ref('')
const jesoosExpandedScenes   = reactive(new Set<number>())
const jesoosJsonDumpExpanded = ref(false)

const scenePayloadRefs: Record<number, HTMLElement> = {}
let   jesoosJsonDumpEl: HTMLElement | null = null

// ── Computed ──────────────────────────────────────────────────────────────────
const jesoosAgendaBrand = computed(() =>
  jesoosAgendasData.value ? Object.keys(jesoosAgendasData.value)[0] : ''
)
const jesoosAgenda = computed(() =>
  jesoosAgendasData.value ? jesoosAgendasData.value[jesoosAgendaBrand.value] : null
)

// ── Helpers ───────────────────────────────────────────────────────────────────
function fmtTimeArr(arr: number[] | undefined | null): string {
  if (!arr || arr.length < 5) return '—'
  return `${String(arr[3]).padStart(2, '0')}:${String(arr[4]).padStart(2, '0')}`
}

function jesoosFormatDuration(sec: number): string {
  const h = Math.floor(sec / 3600), m = Math.floor((sec % 3600) / 60)
  return h > 0 ? `${h}h ${m}m` : `${m}m`
}

function fmtDurSec(sec: number): string {
  const m = Math.floor(sec / 60), s = sec % 60
  return `${m}:${String(s).padStart(2, '0')}`
}

function jesoosSceneType(title: string): string {
  const t = (title ?? '').toLowerCase()
  if (t.includes('news'))                                            return 'news'
  if (t.includes('weather'))                                         return 'weather'
  if (t.includes('greeting') || t.includes('bye') || t.includes('night')) return 'greeting'
  return 'music'
}

function strategyAbbr(s: string): string {
  const map: Record<string, string> = {
    SONG_ONLY:             'only',
    SONG_INTRO_SONG:       'intro',
    INTRO_SONG_INTRO_SONG: 'i·s·i',
    SONG_CROSSFADE_SONG:   'xfade',
  }
  return map[s] ?? s
}

function strategyClass(s: string): string {
  if (s === 'SONG_CROSSFADE_SONG')   return 'strat-xfade'
  if (s === 'INTRO_SONG_INTRO_SONG') return 'strat-isi'
  if (s === 'SONG_INTRO_SONG')       return 'strat-intro'
  return 'strat-only'
}

function batchGroups(timeline: any[]): Array<{ batchId: number; songs: any[] }> {
  const map = new Map<number, any[]>()
  for (const item of (timeline ?? [])) {
    const b = item.batchId ?? 0
    if (!map.has(b)) map.set(b, [])
    map.get(b)!.push(item)
  }
  return Array.from(map.entries()).map(([batchId, songs]) => ({ batchId, songs }))
}

function copyJson(btn: EventTarget | null, json: string) {
  const el = btn as HTMLButtonElement
  navigator.clipboard.writeText(json ?? '').then(() => {
    const orig = el.textContent ?? ''; el.textContent = 'copied'
    setTimeout(() => { el.textContent = orig }, 1800)
  })
}

// ── Scene expand/collapse ─────────────────────────────────────────────────────
function setJesoosSceneRef(idx: number, el: any) {
  if (el) scenePayloadRefs[idx] = el as HTMLElement
  else    delete scenePayloadRefs[idx]
}

function jesoosToggleScene(idx: number) {
  const el = scenePayloadRefs[idx]; if (!el) return
  if (jesoosExpandedScenes.has(idx)) {
    gsap.to(el, { height: 0, duration: 0.3, ease: 'power2.inOut', onComplete() { jesoosExpandedScenes.delete(idx) } })
  } else {
    jesoosExpandedScenes.add(idx)
    nextTick(() => {
      const h = el.scrollHeight
      gsap.fromTo(el, { height: 0 }, { height: h, duration: 0.35, ease: 'power2.out', onComplete() { gsap.set(el, { height: 'auto' }) } })
      gsap.fromTo(el.querySelectorAll('.song-row'), { opacity: 0, x: -10 }, { opacity: 1, x: 0, duration: 0.2, stagger: 0.018, ease: 'power2.out' })
    })
  }
}

function jesoosToggleJsonDump() {
  const el = jesoosJsonDumpEl; if (!el) return
  if (jesoosJsonDumpExpanded.value) {
    gsap.to(el, { height: 0, duration: 0.3, ease: 'power2.inOut', onComplete() { jesoosJsonDumpExpanded.value = false } })
  } else {
    jesoosJsonDumpExpanded.value = true
    nextTick(() => {
      const h = el.scrollHeight
      gsap.fromTo(el, { height: 0 }, { height: h, duration: 0.35, ease: 'power2.out', onComplete() { gsap.set(el, { height: 'auto' }) } })
    })
  }
}

// ── API actions ───────────────────────────────────────────────────────────────
async function jesoosFetchAgendas() {
  jesoosAgendasLoading.value = true; jesoosAgendasBadge.value = 'pending'
  jesoosExpandedScenes.clear(); jesoosJsonDumpExpanded.value = false
  try {
    const res  = await fetch('/jesoos/agendas')
    const data = await res.json()
    jesoosAgendasData.value = data; jesoosAgendasBadge.value = res.ok ? 'ok' : 'err'
    nextTick(() => {
      gsap.from('.agenda-header', { opacity: 0, y: -10, duration: 0.3 })
      gsap.from('.scene-card',    { opacity: 0, y: 20,  duration: 0.3, stagger: 0.03, ease: 'power2.out' })
    })
  } catch (e: any) {
    jesoosAgendasData.value = null; jesoosAgendasBadge.value = 'err'
  } finally { jesoosAgendasLoading.value = false }
}
</script>

<template>
  <main class="jesoos-main">
    <div class="actions-row">
      <button class="action-btn secondary" @click="jesoosFetchAgendas">view agendas</button>
    </div>

    <div class="result-panel">
      <div class="panel-header">
        <span class="panel-title">agendas</span>
        <span v-if="jesoosAgendasBadge" class="panel-badge" :class="jesoosAgendasBadge">
          {{ jesoosAgendasBadge === 'ok' ? 'ok' : jesoosAgendasBadge === 'err' ? 'error' : 'pending' }}
        </span>
      </div>
      <div class="panel-body">
        <div v-if="jesoosAgendasLoading" class="loading-text"><span class="spinner"></span>fetching</div>
        <template v-else-if="jesoosAgendasData && jesoosAgenda">

          <!-- ── Agenda header ── -->
          <div class="agenda-header">
            <div class="agenda-stats">
              <div class="stat-box">
                <span class="stat-label">Brand</span>
                <span class="stat-value">{{ jesoosAgendaBrand }}</span>
              </div>
              <div class="stat-box">
                <span class="stat-label">Scenes</span>
                <span class="stat-value">{{ jesoosAgenda.totalScenes }}</span>
              </div>
              <div class="stat-box">
                <span class="stat-label">Duration</span>
                <span class="stat-value">{{ jesoosFormatDuration(jesoosAgenda.scenes.reduce((a: number, s: any) => a + s.durationSeconds, 0)) }}</span>
              </div>
              <div class="stat-box">
                <span class="stat-label">Songs</span>
                <span class="stat-value">{{ jesoosAgenda.scenes.reduce((a: number, s: any) => a + s.totalSongs, 0) }}</span>
              </div>
            </div>
          </div>

          <!-- ── Scenes ── -->
          <div class="scenes-list">
            <div v-for="(scene, idx) in jesoosAgenda.scenes" :key="scene.id"
              class="scene-card" :class="{ expanded: jesoosExpandedScenes.has(idx) }">

              <!-- scene row header -->
              <div class="scene-row" @click="jesoosToggleScene(idx)">
                <span class="scene-time">
                  {{ fmtTimeArr(scene.scheduledStartTime) }}<span class="scene-time-sep">→</span>{{ fmtTimeArr(scene.scheduledEndTime) }}
                </span>
                <span class="scene-type-badge" :class="jesoosSceneType(scene.title)">{{ jesoosSceneType(scene.title) }}</span>
                <span class="scene-title">{{ scene.title }}</span>
                <span class="scene-duration">{{ jesoosFormatDuration(scene.durationSeconds) }}</span>
                <span class="scene-songs" :class="{ 'scene-songs-empty': scene.totalSongs === 0 }">
                  {{ scene.totalSongs }} songs
                </span>
                <span class="chevron-cell" :class="{ 'chevron-open': jesoosExpandedScenes.has(idx) }">›</span>
              </div>

              <!-- expanded: song timeline -->
              <div class="scene-payload" :class="{ open: jesoosExpandedScenes.has(idx) }"
                :ref="(el: any) => setJesoosSceneRef(idx, el)">

                <!-- no songs -->
                <div v-if="scene.totalSongs === 0" class="timeline-empty">
                  no songs in this scene
                </div>

                <!-- song timeline -->
                <template v-else>
                  <div class="timeline-bar">
                    <span class="tl-stat">{{ scene.totalSongs }} songs</span>
                    <span class="tl-dot">·</span>
                    <span class="tl-stat">{{ jesoosFormatDuration(scene.durationSeconds) }}</span>
                    <span class="tl-dot">·</span>
                    <span class="tl-stat tl-built" :class="{ 'tl-built-ok': scene.timelineBuilt }">
                      {{ scene.timelineBuilt ? 'timeline built' : 'not built' }}
                    </span>
                    <span class="tl-dot">·</span>
                    <span class="tl-stat">{{ batchGroups(scene.timeline).length }} batches</span>
                  </div>

                  <div class="timeline-groups">
                    <div v-for="group in batchGroups(scene.timeline)" :key="group.batchId" class="batch-group">
                      <div class="batch-label">batch {{ group.batchId }}</div>
                      <div v-for="song in group.songs" :key="song.id" class="song-row">
                        <span class="song-seq">#{{ song.sequenceNumber }}</span>
                        <span class="song-emit-time">{{ fmtTimeArr(song.scheduledEmissionTime) }}</span>
                        <div class="song-info">
                          <span class="song-title">{{ song.songTitle }}</span>
                          <span class="song-artist">{{ song.artist }}</span>
                        </div>
                        <span class="song-dur">{{ fmtDurSec(song.durationSeconds) }}</span>
                        <span class="song-strategy" :class="strategyClass(song.mixingStrategy)">
                          {{ strategyAbbr(song.mixingStrategy) }}
                        </span>
                        <span class="song-flags">
                          <span v-if="song.hasIntro"  class="flag flag-intro">I</span>
                          <span v-if="song.hasJingle" class="flag flag-jingle">J</span>
                        </span>
                      </div>
                    </div>
                  </div>
                </template>
              </div>
            </div>
          </div>

          <!-- ── Full JSON dump ── -->
          <div class="json-dump-section" :class="{ expanded: jesoosJsonDumpExpanded }">
            <div class="json-dump-header" @click="jesoosToggleJsonDump">
              <span class="json-dump-title">full json dump</span>
              <span class="json-dump-chevron">›</span>
            </div>
            <div class="json-dump-body" :ref="(el: any) => { jesoosJsonDumpEl = el }" :class="{ open: jesoosJsonDumpExpanded }">
              <div class="json-dump-inner">
                <div class="payload-json-wrap">
                  <pre class="json-code">{{ JSON.stringify(jesoosAgendasData, null, 2) }}</pre>
                  <button class="copy-btn" @click="copyJson($event.target, JSON.stringify(jesoosAgendasData, null, 2))">copy all</button>
                </div>
              </div>
            </div>
          </div>

        </template>
        <div v-else class="panel-empty">no result yet</div>
      </div>
    </div>
  </main>
</template>

<style scoped>
/* ── Scene row ── */
.scene-time { font-family: var(--mono); font-size: 0.68rem; font-weight: 600; letter-spacing: 0.5px; color: var(--accent2); white-space: nowrap; }
.scene-time-sep { color: var(--text-dim); margin: 0 4px; font-weight: 400; }
.scene-row { grid-template-columns: max-content max-content 1fr max-content max-content auto; }
.scene-songs-empty { color: var(--text-dim); border-color: rgba(255,255,255,0.06); }
.chevron-cell { transition: transform 0.25s; display: inline-block; }
.chevron-open { transform: rotate(90deg); }

/* ── Timeline bar ── */
.timeline-bar {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 16px;
  background: rgba(0,0,0,0.2);
  border-bottom: 1px solid var(--border);
  font-family: var(--mono); font-size: 0.62rem; letter-spacing: 0.5px;
}
.tl-stat { color: var(--text-dim); }
.tl-dot  { color: var(--border-bright); }
.tl-built-ok { color: var(--green); }

/* ── Timeline groups ── */
.timeline-groups { padding: 12px 16px; display: flex; flex-direction: column; gap: 8px; }

.batch-group { display: flex; flex-direction: column; gap: 1px; }

.batch-label {
  font-family: var(--mono); font-size: 0.58rem; letter-spacing: 1.5px; text-transform: uppercase;
  color: var(--text-dim); padding: 4px 6px 2px;
  border-left: 2px solid var(--border-bright); margin-bottom: 2px; margin-left: 2px;
}

.song-row {
  display: grid;
  grid-template-columns: 28px 44px 1fr 42px 48px 36px;
  align-items: center; gap: 10px;
  padding: 6px 10px; border-radius: 4px;
  background: rgba(255,255,255,0.02);
  border: 1px solid transparent;
  transition: border-color 0.15s, background 0.15s;
}
.song-row:hover { background: rgba(255,255,255,0.04); border-color: var(--border); }

.song-seq       { font-family: var(--mono); font-size: 0.6rem; color: var(--text-dim); text-align: right; }
.song-emit-time { font-family: var(--mono); font-size: 0.62rem; color: var(--accent2); letter-spacing: 0.5px; }

.song-info      { display: flex; flex-direction: column; gap: 1px; overflow: hidden; }
.song-title     { font-size: 0.8rem; color: var(--text); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.song-artist    { font-size: 0.65rem; color: var(--text-dim); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

.song-dur       { font-family: var(--mono); font-size: 0.62rem; color: var(--text-muted); text-align: right; }

.song-strategy  {
  font-family: var(--mono); font-size: 0.58rem; letter-spacing: 0.5px;
  padding: 2px 6px; border-radius: 3px; border: 1px solid; text-align: center;
  white-space: nowrap;
}
.strat-only  { color: var(--text-dim);  border-color: rgba(255,255,255,0.1); background: transparent; }
.strat-intro { color: var(--accent);    border-color: rgba(33,150,243,0.3);  background: rgba(33,150,243,0.06); }
.strat-isi   { color: var(--purple);    border-color: rgba(176,132,255,0.3); background: rgba(176,132,255,0.06); }
.strat-xfade { color: var(--amber);     border-color: rgba(245,166,35,0.3);  background: rgba(245,166,35,0.06); }

.song-flags { display: flex; gap: 3px; }
.flag {
  font-family: var(--mono); font-size: 0.58rem; font-weight: 600;
  width: 16px; height: 16px; border-radius: 3px; display: flex; align-items: center; justify-content: center;
}
.flag-intro  { background: rgba(33,150,243,0.2); color: var(--accent2); }
.flag-jingle { background: rgba(245,166,35,0.2);  color: var(--amber); }

/* ── Empty timeline ── */
.timeline-empty {
  padding: 14px 16px;
  font-family: var(--mono); font-size: 0.65rem; letter-spacing: 1px;
  color: var(--text-dim); text-transform: uppercase;
}
</style>
