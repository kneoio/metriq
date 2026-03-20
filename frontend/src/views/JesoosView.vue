<script setup lang="ts">
import { ref, reactive, computed, nextTick } from 'vue'
import gsap from 'gsap'
import { STATION_LIST } from '@/utils/service'

// ── State ─────────────────────────────────────────────────────────────────────
const jesoosBrand            = ref('lumisonic')
const jesoosStatus           = ref('idle')
const jesoosStartLoading     = ref(false)
const jesoosStartResult      = ref<unknown>(null)
const jesoosStartBadge       = ref('')
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
function jesoosFormatDuration(sec: number): string {
  const h = Math.floor(sec / 3600), m = Math.floor((sec % 3600) / 60)
  return h > 0 ? `${h}h ${m}m` : `${m}m`
}
function jesoosFormatTime(dt: string): string {
  return new Date(dt).toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', hour12: false })
}
function jesoosSceneType(title: string): string {
  const t = (title ?? '').toLowerCase()
  if (t.includes('news'))                      return 'news'
  if (t.includes('weather'))                   return 'weather'
  if (t.includes('greeting') || t.includes('bye')) return 'greeting'
  return 'music'
}

function copyJson(btn: EventTarget | null, json: string) {
  const el = btn as HTMLButtonElement
  navigator.clipboard.writeText(json ?? '').then(() => {
    const orig = el.textContent ?? ''
    el.textContent = 'copied'
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
      gsap.fromTo(el.querySelectorAll('.meta-pair,.payload-json-wrap'), { opacity: 0, y: 8 }, { opacity: 1, y: 0, duration: 0.25, stagger: 0.04 })
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
async function jesoosStart() {
  jesoosStartLoading.value = true
  jesoosStartBadge.value   = 'pending'
  jesoosStatus.value       = 'running'
  try {
    const res  = await fetch(`/jesoos/${jesoosBrand.value}/start`, { method: 'POST', headers: { 'Content-Type': 'application/json' } })
    const data = await res.json()
    jesoosStartResult.value = data
    jesoosStartBadge.value  = res.ok ? 'ok' : 'err'
    jesoosStatus.value      = res.ok ? 'running' : 'error'
  } catch (e: any) {
    jesoosStartResult.value = e.message
    jesoosStartBadge.value  = 'err'
    jesoosStatus.value      = 'error'
  } finally { jesoosStartLoading.value = false }
}

async function jesoosFetchAgendas() {
  jesoosAgendasLoading.value = true
  jesoosAgendasBadge.value   = 'pending'
  jesoosExpandedScenes.clear()
  jesoosJsonDumpExpanded.value = false
  try {
    const res  = await fetch('/jesoos/agendas')
    const data = await res.json()
    jesoosAgendasData.value  = data
    jesoosAgendasBadge.value = res.ok ? 'ok' : 'err'
    nextTick(() => {
      gsap.from('.agenda-header', { opacity: 0, y: -10, duration: 0.3 })
      gsap.from('.scene-card',    { opacity: 0, y: 20,  duration: 0.3, stagger: 0.03, ease: 'power2.out' })
    })
  } catch (e: any) {
    jesoosAgendasData.value  = null
    jesoosStartResult.value  = e.message
    jesoosAgendasBadge.value = 'err'
  } finally { jesoosAgendasLoading.value = false }
}
</script>

<template>
  <!-- Sidebar content -->
  <Teleport to="#view-sidebar">
    <div class="sidebar-section">
      <span class="section-label">Brand</span>
      <div class="select-wrap">
        <select class="station-select" v-model="jesoosBrand">
          <option v-for="s in STATION_LIST" :key="s" :value="s">{{ s }}</option>
        </select>
      </div>
    </div>
    <div class="status-indicator" style="margin-top:0; border-top: 1px solid var(--border);">
      <div class="status-row">
        <div class="status-dot" :class="jesoosStatus"></div>
        <span class="status-text">{{ jesoosStatus }}</span>
      </div>
    </div>
  </Teleport>

  <!-- Main content -->
  <main class="jesoos-main">
    <div class="actions-row">
      <button class="action-btn primary" @click="jesoosStart" :disabled="jesoosStartLoading">▶ start</button>
      <button class="action-btn secondary" @click="jesoosFetchAgendas">view agendas</button>
    </div>

    <!-- Start result panel -->
    <div class="result-panel">
      <div class="panel-header">
        <span class="panel-title">start result</span>
        <span v-if="jesoosStartBadge" class="panel-badge" :class="jesoosStartBadge">
          {{ jesoosStartBadge === 'ok' ? 'ok' : jesoosStartBadge === 'err' ? 'error' : 'pending' }}
        </span>
      </div>
      <div class="panel-body">
        <div v-if="jesoosStartLoading" class="loading-text"><span class="spinner"></span>fetching</div>
        <pre v-else-if="jesoosStartResult !== null" class="json-code"
          :style="jesoosStartBadge === 'err' ? 'color:var(--accent3)' : ''">{{ typeof jesoosStartResult === 'string' ? jesoosStartResult : JSON.stringify(jesoosStartResult, null, 2) }}</pre>
        <div v-else class="panel-empty">no result yet</div>
      </div>
    </div>

    <!-- Agendas panel -->
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
          <div class="scenes-list">
            <div v-for="(scene, idx) in jesoosAgenda.scenes" :key="scene.id"
              class="scene-card" :class="{ expanded: jesoosExpandedScenes.has(idx) }">
              <div class="scene-row" @click="jesoosToggleScene(idx)">
                <span class="scene-time">{{ jesoosFormatTime(scene.scheduledStartTime) }}</span>
                <span class="scene-type-badge" :class="jesoosSceneType(scene.title)">{{ jesoosSceneType(scene.title) }}</span>
                <span class="scene-title">{{ scene.title }}</span>
                <span class="scene-duration">{{ jesoosFormatDuration(scene.durationSeconds) }}</span>
                <span class="scene-songs">{{ scene.totalSongs }} songs</span>
                <span class="chevron-cell">›</span>
              </div>
              <div class="scene-payload" :class="{ open: jesoosExpandedScenes.has(idx) }"
                :ref="(el: any) => setJesoosSceneRef(idx, el)">
                <div class="payload-inner">
                  <div class="payload-meta">
                    <div class="meta-pair"><span class="meta-key">scene id</span><span class="meta-val">{{ scene.id }}</span></div>
                    <div class="meta-pair"><span class="meta-key">duration</span><span class="meta-val">{{ scene.durationSeconds }}s</span></div>
                    <div class="meta-pair"><span class="meta-key">songs</span><span class="meta-val">{{ scene.totalSongs }}</span></div>
                    <div class="meta-pair"><span class="meta-key">start time</span><span class="meta-val">{{ scene.scheduledStartTime }}</span></div>
                  </div>
                  <div class="payload-json-wrap">
                    <pre class="payload-json">{{ JSON.stringify(scene, null, 2) }}</pre>
                    <button class="copy-btn" @click.stop="copyJson($event.target, JSON.stringify(scene, null, 2))">copy</button>
                  </div>
                </div>
              </div>
            </div>
          </div>
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
