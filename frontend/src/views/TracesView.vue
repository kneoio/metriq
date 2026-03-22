<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import gsap from 'gsap'
import { useMetriqStore } from '@/stores/metriq'
import { useTracesStore } from '@/stores/traces'
import { servicePillHtml, isError, isDebug } from '@/utils/service'
import { relTime, flowTimeDelta } from '@/utils/time'
import type { EventEntry } from '@/types'

const metriq = useMetriqStore()
const traces = useTracesStore()

const flowContainerEl = ref<HTMLElement | null>(null)
const dialogEntry     = ref<EventEntry | null>(null)
const copyLabel         = ref('COPY')
const snapshotLabel     = ref('SNAPSHOT')

const eventsForSelectedTrace = computed((): EventEntry[] => {
  if (!traces.selectedTraceId) return []
  return ((metriq.byTrace[traces.selectedTraceId] ?? []) as EventEntry[])
    .slice()
    .sort((a, b) => a.receivedAt.getTime() - b.receivedAt.getTime())
})

watch(() => traces.selectedTraceId, () => { dialogEntry.value = null })

watch(() => eventsForSelectedTrace.value.length, (len, prev) => {
  if (len > (prev ?? 0)) {
    nextTick(() => {
      if (!flowContainerEl.value) return
      const nodes = flowContainerEl.value.querySelectorAll('.flow-node')
      const last  = nodes[nodes.length - 1]
      if (last) gsap.fromTo(last, { opacity: 0, x: 20 }, { opacity: 1, x: 0, duration: 0.35, ease: 'power3.out' })
    })
  }
})

function openDialog(entry: EventEntry) {
  dialogEntry.value = entry
  copyLabel.value = 'COPY'
}

function closeDialog() {
  dialogEntry.value = null
}

function copyJson() {
  if (!dialogEntry.value) return
  const json = JSON.stringify(dialogEntry.value.data.payload ?? dialogEntry.value.data, null, 2)
  navigator.clipboard.writeText(json).then(() => {
    copyLabel.value = 'COPIED'
    setTimeout(() => { copyLabel.value = 'COPY' }, 1800)
  })
}

function copySnapshot() {
  const events = eventsForSelectedTrace.value
  const brand  = events.find(e => e.data.brandName)?.data.brandName ?? null
  const snapshot = {
    traceId:    traces.selectedTraceId,
    brand,
    snapshotAt: new Date().toISOString(),
    eventCount: events.length,
    events: events.map((e, idx) => ({
      seq:       idx + 1,
      receivedAt: e.receivedAt.toISOString(),
      type:      e.data.type ?? null,
      brandName: e.data.brandName ?? null,
      serviceId: e.data.serviceId ?? null,
      code:      e.data.code ?? null,
      payload:   e.data.payload ?? e.data,
    })),
  }
  navigator.clipboard.writeText(JSON.stringify(snapshot, null, 2)).then(() => {
    snapshotLabel.value = 'COPIED!'
    setTimeout(() => { snapshotLabel.value = 'SNAPSHOT' }, 1800)
  })
}

function deltaMs(prev: EventEntry, curr: EventEntry): string {
  return flowTimeDelta(prev.receivedAt.getTime(), curr.receivedAt.getTime())
}

const dialogJson = computed(() => {
  if (!dialogEntry.value) return ''
  return JSON.stringify(dialogEntry.value.data.payload ?? dialogEntry.value.data, null, 2)
})
</script>

<template>
  <main class="traces-main">
    <div v-if="!traces.selectedTraceId || eventsForSelectedTrace.length === 0" class="empty-state">
      <div class="empty-icon">⬡</div>
      <div class="empty-text">{{ traces.selectedTraceId ? 'no events' : 'select a trace' }}</div>
    </div>
    <template v-else>
      <div class="trace-header-bar">
        <span class="trace-header-label">trace</span>
        <span class="trace-header-id">{{ traces.selectedTraceId }}</span>
        <span class="trace-event-count">{{ eventsForSelectedTrace.length }} events</span>
        <div class="trace-header-actions">
          <button class="action-btn"
            :style="traces.showFlowTiming ? 'border-color:var(--accent);color:var(--accent)' : ''"
            @click="traces.showFlowTiming = !traces.showFlowTiming">⏱ TIMING</button>
          <button class="action-btn" @click="copySnapshot">{{ snapshotLabel }}</button>
        </div>
      </div>
      <div class="flow-scroll">
        <div class="flow-container" ref="flowContainerEl">
          <template v-for="(entry, idx) in eventsForSelectedTrace" :key="entry.id">
            <div class="flow-arrow" v-if="idx > 0">
              <span>→</span>
              <span v-if="traces.showFlowTiming" class="flow-arrow-time">{{ deltaMs(eventsForSelectedTrace[idx - 1], entry) }}</span>
            </div>
            <div class="flow-node"
              :class="{ 'is-error': isError(entry.data.type as string), 'is-debug': isDebug(entry.data.type as string) }"
              @click="openDialog(entry)" style="cursor:pointer;">
              <div class="flow-node-header">
                <div class="flow-node-seq">#{{ idx + 1 }}</div>
                <div class="flow-node-type"
                  :style="isError(entry.data.type as string) ? 'color:var(--accent3)' : isDebug(entry.data.type as string) ? 'color:var(--text-dim)' : ''">
                  {{ (entry.data.type || 'UNKNOWN').toUpperCase() }}</div>
                <span v-html="servicePillHtml(entry.data.serviceId as string)"></span>
                <div class="flow-node-brand" v-if="entry.data.brandName">{{ entry.data.brandName }}</div>
                <div class="flow-node-code" v-if="entry.data.code">{{ entry.data.code }}</div>
                <div class="flow-node-time">{{ relTime(entry.receivedAt) }}</div>
              </div>
            </div>
          </template>
        </div>
      </div>
    </template>

    <!-- ── Modal dialog ── -->
    <Teleport to="body">
      <div v-if="dialogEntry" class="modal-backdrop" @click.self="closeDialog">
        <div class="modal-box">
          <div class="modal-header">
            <div class="modal-meta">
              <span class="modal-type"
                :style="isError(dialogEntry.data.type as string) ? 'color:var(--accent3)' : isDebug(dialogEntry.data.type as string) ? 'color:var(--text-dim)' : 'color:var(--accent)'">
                {{ (dialogEntry.data.type || 'UNKNOWN').toUpperCase() }}
              </span>
              <span v-html="servicePillHtml(dialogEntry.data.serviceId as string)"></span>
              <span v-if="dialogEntry.data.brandName" class="modal-brand">{{ dialogEntry.data.brandName }}</span>
              <span v-if="dialogEntry.data.code" class="modal-code">{{ dialogEntry.data.code }}</span>
              <span class="modal-time">{{ dialogEntry.receivedAt.toLocaleTimeString() }}</span>
            </div>
            <div class="modal-actions">
              <button class="action-btn" @click="copyJson">{{ copyLabel }}</button>
              <button class="modal-close" @click="closeDialog">✕</button>
            </div>
          </div>
          <pre class="modal-json">{{ dialogJson }}</pre>
        </div>
      </div>
    </Teleport>
  </main>
</template>

<style scoped>
.trace-header-actions { display: flex; align-items: center; gap: 6px; margin-left: auto; }

.modal-backdrop {
  position: fixed; inset: 0; z-index: 1000;
  background: rgba(0,0,0,0.65); backdrop-filter: blur(4px);
  display: flex; align-items: center; justify-content: center;
}
.modal-box {
  background: var(--surface); border: 1px solid var(--border-bright);
  border-radius: 6px; width: min(720px, 92vw); max-height: 80vh;
  display: flex; flex-direction: column; overflow: hidden;
  box-shadow: 0 24px 64px rgba(0,0,0,0.6);
}
.modal-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 16px; border-bottom: 1px solid var(--border); gap: 12px; flex-wrap: wrap;
}
.modal-meta { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.modal-type { font-family: var(--mono); font-size: 0.7rem; font-weight: 600; letter-spacing: 1px; }
.modal-brand { font-family: var(--mono); font-size: 0.65rem; color: var(--text-muted); letter-spacing: 0.5px; }
.modal-code { font-family: var(--mono); font-size: 0.65rem; color: var(--amber); }
.modal-time { font-family: var(--mono); font-size: 0.6rem; color: var(--text-dim); }
.modal-actions { display: flex; align-items: center; gap: 8px; }
.modal-close {
  background: none; border: 1px solid var(--border); color: var(--text-dim);
  font-size: 0.7rem; cursor: pointer; padding: 3px 8px; border-radius: 3px;
  transition: all 0.15s;
}
.modal-close:hover { border-color: var(--accent3); color: var(--accent3); }
.modal-json {
  font-family: var(--mono); font-size: 0.68rem; line-height: 1.6;
  color: var(--text-muted); padding: 16px; overflow: auto;
  white-space: pre; flex: 1;
}
</style>
