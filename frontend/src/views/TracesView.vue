<script setup lang="ts">
import { reactive, computed, watch, nextTick } from 'vue'
import gsap from 'gsap'
import { useMetriqStore } from '@/stores/metriq'
import { useTracesStore } from '@/stores/traces'
import { servicePillHtml, isError } from '@/utils/service'
import { relTime, flowTimeDelta } from '@/utils/time'
import type { EventEntry } from '@/types'

const metriq = useMetriqStore()
const traces = useTracesStore()

// ── State ─────────────────────────────────────────────────────────────────────
const expandedTraceIds = reactive(new Set<number>())
const flowContainerEl  = ref<HTMLElement | null>(null)

import { ref } from 'vue'

// ── Computed ──────────────────────────────────────────────────────────────────
const eventsForSelectedTrace = computed((): EventEntry[] => {
  if (!traces.selectedTraceId) return []
  return ((metriq.byTrace[traces.selectedTraceId] ?? []) as EventEntry[])
    .slice()
    .sort((a, b) => a.receivedAt.getTime() - b.receivedAt.getTime())
})

// Reset expanded trace nodes when trace changes
watch(() => traces.selectedTraceId, () => { expandedTraceIds.clear() })

// ── GSAP: animate newest flow node ────────────────────────────────────────────
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

// ── Methods ───────────────────────────────────────────────────────────────────
function toggleFlowNode(id: number) {
  if (expandedTraceIds.has(id)) expandedTraceIds.delete(id)
  else expandedTraceIds.add(id)
}

function copyJson(btn: EventTarget | null, json: string) {
  const el = btn as HTMLButtonElement
  navigator.clipboard.writeText(json ?? '').then(() => {
    const orig = el.textContent ?? ''
    el.textContent = 'copied'
    setTimeout(() => { el.textContent = orig }, 1800)
  })
}

function deltaMs(prev: EventEntry, curr: EventEntry): string {
  return flowTimeDelta(prev.receivedAt.getTime(), curr.receivedAt.getTime())
}
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
      </div>
      <div class="flow-scroll">
        <div class="flow-container" ref="flowContainerEl">
          <template v-for="(entry, idx) in eventsForSelectedTrace" :key="entry.id">
            <div class="flow-arrow" v-if="idx > 0">
              <span>→</span>
              <span v-if="traces.showFlowTiming" class="flow-arrow-time">{{ deltaMs(eventsForSelectedTrace[idx - 1], entry) }}</span>
            </div>
            <div class="flow-node"
              :class="{ expanded: expandedTraceIds.has(entry.id), 'is-error': isError(entry.data.type as string) }">
              <div class="flow-node-header" @click="toggleFlowNode(entry.id)">
                <div class="flow-node-seq">#{{ idx + 1 }}</div>
                <div class="flow-node-type" :style="isError(entry.data.type as string) ? 'color:var(--accent3)' : ''">
                  {{ (entry.data.type || 'UNKNOWN').toUpperCase() }}</div>
                <span v-html="servicePillHtml(entry.data.serviceId as string)"></span>
                <div class="flow-node-code" v-if="entry.data.code">{{ entry.data.code }}</div>
                <div class="flow-node-time">{{ relTime(entry.receivedAt) }}</div>
              </div>
              <div v-if="expandedTraceIds.has(entry.id)" class="flow-node-payload">
                <div class="flow-payload-meta">
                  <div class="meta-pair"><span class="meta-key">trace</span><span class="meta-val" style="font-size:0.6rem;word-break:break-all;">{{ entry.data.traceId || '—' }}</span></div>
                  <div class="meta-pair"><span class="meta-key">code</span><span class="meta-val">{{ entry.data.code || '—' }}</span></div>
                  <div class="meta-pair"><span class="meta-key">time</span><span class="meta-val">{{ entry.receivedAt.toLocaleTimeString() }}</span></div>
                </div>
                <div v-if="entry.data.payload" class="payload-json-wrap">
                  <pre class="payload-json" style="font-size:0.65rem;max-height:200px;">{{ JSON.stringify(entry.data.payload, null, 2) }}</pre>
                  <button class="copy-btn" @click.stop="copyJson($event.target, JSON.stringify(entry.data.payload, null, 2))">copy</button>
                </div>
              </div>
            </div>
          </template>
        </div>
      </div>
    </template>
  </main>
</template>
