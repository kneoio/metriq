<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import gsap from 'gsap'
import { useMetriqStore } from '@/stores/metriq'
import { useTracesStore } from '@/stores/traces'
import { useContextStore } from '@/stores/context'
import { servicePillHtml, metricEventTypeClass } from '@/utils/service'
import { relTime, flowTimeDelta } from '@/utils/time'
import type { EventEntry } from '@/types'

const metriq   = useMetriqStore()
const traces   = useTracesStore()
const context  = useContextStore()

const flowContainerEl = ref<HTMLElement | null>(null)
const snapshotLabel   = ref('SNAPSHOT')

const eventsForSelectedTrace = computed((): EventEntry[] => {
  if (!traces.selectedTraceId) return []
  return ((metriq.byTrace[traces.selectedTraceId] ?? []) as EventEntry[])
    .filter(e =>
      (e.data.brandName   ?? '').trim().toUpperCase() === context.activeBrand.trim().toUpperCase() &&
      (e.data.processType ?? '').toUpperCase() === 'FLOW'
    )
    .slice()
    .sort((a, b) => a.receivedAt.getTime() - b.receivedAt.getTime())
})

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

function copySnapshot() {
  const events = eventsForSelectedTrace.value
  const brand  = events.find(e => e.data.brandName)?.data.brandName ?? null
  const snapshot = {
    traceId:    traces.selectedTraceId,
    brand,
    snapshotAt: new Date().toISOString(),
    eventCount: events.length,
    events: events.map((e, idx) => ({
      seq:         idx + 1,
      receivedAt:  e.receivedAt.toISOString(),
      type:        e.data.type        ?? null,
      processType: e.data.processType ?? null,
      brandName:   e.data.brandName   ?? null,
      serviceId:   e.data.serviceId   ?? null,
      code:        e.data.code        ?? null,
      payload:     e.data.payload     ?? e.data,
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

function formatPayload(entry: EventEntry): string {
  const p = (entry.data as any).payload
  if (p == null) return ''
  return typeof p === 'string' ? p : JSON.stringify(p, null, 2)
}

const copiedId     = ref<number | null>(null)
const copiedCodeId = ref<number | null>(null)

function copyCode(entry: EventEntry) {
  const code = (entry.data as any).code
  if (!code) return
  navigator.clipboard.writeText(code).then(() => {
    copiedCodeId.value = entry.id
    setTimeout(() => { copiedCodeId.value = null }, 1200)
  })
}

function copyEvent(entry: EventEntry) {
  const d = entry.data as any
  const obj = {
    receivedAt:  entry.receivedAt.toISOString(),
    type:        d.type        ?? null,
    processType: d.processType ?? null,
    brandName:   d.brandName   ?? null,
    serviceId:   d.serviceId   ?? null,
    code:        d.code        ?? null,
    payload:     d.payload     ?? null,
  }
  navigator.clipboard.writeText(JSON.stringify(obj, null, 2)).then(() => {
    copiedId.value = entry.id
    setTimeout(() => { copiedId.value = null }, 1500)
  })
}

</script>

<template>
  <main class="traces-main">
    <div v-if="!traces.selectedTraceId || eventsForSelectedTrace.length === 0" class="empty-state">
      <div class="empty-icon">⬡</div>
      <div class="empty-text">{{ traces.selectedTraceId ? 'no flow events' : 'select a trace' }}</div>
    </div>
    <template v-else>
      <div class="trace-header-bar">
        <span class="trace-header-label">trace</span>
        <span class="trace-header-id station-name">{{ context.activeBrand }}</span>
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
            <div class="flow-node">
              <div class="flow-node-header">
                <div class="node-row1">
                  <span class="metric-type-badge metric-type-badge--compact" :class="metricEventTypeClass(entry.data.type as string)">
                    {{ (entry.data.type || 'UNKNOWN').toUpperCase() }}
                  </span>
                  <span v-html="servicePillHtml(entry.data.serviceId as string)"></span>
                  <div class="flow-node-brand" v-if="entry.data.brandName">{{ entry.data.brandName }}</div>
                  <button class="copy-event-btn" @click.stop="copyEvent(entry)" :class="{ copied: copiedId === entry.id }">
                    {{ copiedId === entry.id ? '✓' : '⎘' }}
                  </button>
                </div>
                <div class="node-row2">
                  <div class="flow-node-code" v-if="entry.data.code" :class="{ 'code-copied': copiedCodeId === entry.id }" @click.stop="copyCode(entry)">{{ entry.data.code }}</div>
                  <div class="flow-node-time">{{ relTime(entry.receivedAt) }}</div>
                </div>
              </div>
              <div class="flow-node-payload">
                <pre class="payload-pre">{{ formatPayload(entry) }}</pre>
              </div>
            </div>
          </template>
        </div>
      </div>
    </template>
  </main>
</template>

<style scoped>
.trace-header-actions { display: flex; align-items: center; gap: 6px; margin-left: auto; }
.node-row1, .node-row2 { display: flex; align-items: center; gap: 8px; }

/* override global 190px fixed width — let payload determine card width */
:deep(.flow-node) {
  width: auto;
  min-width: 260px;
  max-width: 600px;
}

.copy-event-btn {
  margin-left: auto;
  background: transparent;
  border: 1px solid var(--border);
  color: var(--text-dim);
  font-size: 0.6rem;
  padding: 1px 6px;
  border-radius: 3px;
  cursor: pointer;
  line-height: 1.6;
  transition: border-color 0.15s, color 0.15s;
}
.copy-event-btn:hover { border-color: var(--border-bright); color: var(--text); }
.copy-event-btn.copied { border-color: var(--green); color: var(--green); }

.flow-node-payload {
  border-top: 1px solid var(--border);
  padding: 10px 14px;
  background: rgba(0,0,0,0.15);
  max-height: none;
  overflow: visible;
}
.payload-pre {
  font-family: var(--mono);
  font-size: 0.55rem;
  line-height: 1.6;
  color: var(--text-muted);
  white-space: pre-wrap;
  word-break: break-all;
  margin: 0;
}
</style>
