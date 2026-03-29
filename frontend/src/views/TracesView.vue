<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import gsap from 'gsap'
import { useMetriqStore } from '@/stores/metriq'
import { useTracesStore } from '@/stores/traces'
import { useContextStore } from '@/stores/context'
import { servicePillHtml, isError, isWarning, isDebug, isImportantInfo } from '@/utils/service'
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
  const { _receivedAt, ...rest } = entry.data as any
  return JSON.stringify(rest, null, 2)
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
              :class="{ 'is-error': isError(entry.data.type as string), 'is-debug': isDebug(entry.data.type as string) }">
              <div class="flow-node-header">
                <div class="flow-node-seq">#{{ idx + 1 }}</div>
                <div class="flow-node-type"
                  :style="isError(entry.data.type as string) ? 'color:var(--accent3)' : isWarning(entry.data.type as string) ? 'color:var(--amber)' : isDebug(entry.data.type as string) ? 'color:var(--text-dim)' : isImportantInfo(entry.data.type as string) ? 'color:var(--cyan)' : ''">
                  {{ (entry.data.type || 'UNKNOWN').toUpperCase() }}
                </div>
                <span v-html="servicePillHtml(entry.data.serviceId as string)"></span>
                <div class="flow-node-brand" v-if="entry.data.brandName">{{ entry.data.brandName }}</div>
                <div class="flow-node-code"  v-if="entry.data.code">{{ entry.data.code }}</div>
                <div class="flow-node-time">{{ relTime(entry.receivedAt) }}</div>
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

/* override global 190px fixed width — let payload determine card width */
:deep(.flow-node) {
  width: auto;
  min-width: 260px;
  max-width: 600px;
}

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
