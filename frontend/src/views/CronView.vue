<script setup lang="ts">
import { ref, computed } from 'vue'
import { useMetriqStore } from '@/stores/metriq'
import { useContextStore } from '@/stores/context'
import { servicePillHtml, isError, isWarning, isDebug, isImportantInfo } from '@/utils/service'
import { relTime } from '@/utils/time'
import type { EventEntry } from '@/types'

const metriq  = useMetriqStore()
const context = useContextStore()

const events = computed((): EventEntry[] =>
  metriq.events.filter(e =>
    (e.data.processType ?? '').toUpperCase() === 'CRON' &&
    (e.data.brandName   ?? '').trim() === context.activeBrand
  )
)

function formatPayload(entry: EventEntry): string {
  const p = (entry.data as any).payload
  if (p == null) return ''
  return typeof p === 'string' ? p : JSON.stringify(p, null, 2)
}

const copiedId      = ref<number | null>(null)
const copiedCodeId  = ref<number | null>(null)
const snapshotLabel = ref('SNAPSHOT')

function copyCode(entry: EventEntry) {
  const code = (entry.data as any).code
  if (!code) return
  navigator.clipboard.writeText(code).then(() => {
    copiedCodeId.value = entry.id
    setTimeout(() => { copiedCodeId.value = null }, 1200)
  })
}

function copySnapshot() {
  const evts = events.value
  const snapshot = {
    snapshotAt:  new Date().toISOString(),
    processType: 'CRON',
    brand:       evts[0]?.data.brandName ?? null,
    eventCount:  evts.length,
    events: evts.map((e, idx) => {
      const d = e.data as any
      return {
        seq:        idx + 1,
        receivedAt: e.receivedAt.toISOString(),
        type:       d.type        ?? null,
        serviceId:  d.serviceId   ?? null,
        code:       d.code        ?? null,
        payload:    d.payload     ?? d,
      }
    }),
  }
  navigator.clipboard.writeText(JSON.stringify(snapshot, null, 2)).then(() => {
    snapshotLabel.value = 'COPIED!'
    setTimeout(() => { snapshotLabel.value = 'SNAPSHOT' }, 1800)
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
    <div v-if="events.length === 0" class="empty-state">
      <div class="empty-icon">⬡</div>
      <div class="empty-text">no cron events</div>
    </div>
    <template v-else>
    <div class="trace-header-bar">
      <span class="trace-header-label">cron</span>
      <span class="trace-header-id">{{ context.activeBrand }}</span>
      <span class="trace-event-count">{{ events.length }} events</span>
      <div class="trace-header-actions">
        <button class="action-btn" @click="copySnapshot">{{ snapshotLabel }}</button>
      </div>
    </div>
    <div class="flow-scroll">
      <div class="flow-container">
        <template v-for="(entry, idx) in events" :key="entry.id">
        <div v-if="idx > 0" class="flow-arrow"><span>→</span></div>
        <div class="flow-node">
          <div class="flow-node-header">
            <div class="node-row1">
              <span class="node-type"
                :style="isError(entry.data.type as string) ? 'color:var(--accent3)' : isWarning(entry.data.type as string) ? 'color:var(--amber)' : isDebug(entry.data.type as string) ? 'color:var(--text-dim)' : isImportantInfo(entry.data.type as string) ? 'color:var(--cyan)' : ''">
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
.node-row1, .node-row2 { display: flex; align-items: center; gap: 8px; }
.node-type { font-family: var(--mono); font-size: 0.52rem; letter-spacing: 1px; color: var(--text-muted); }
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
.copy-event-btn:hover  { border-color: var(--border-bright); color: var(--text); }
.copy-event-btn.copied { border-color: var(--green); color: var(--green); }

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
