<script setup lang="ts">
import { computed } from 'vue'
import { useMetriqStore } from '@/stores/metriq'
import { useContextStore } from '@/stores/context'
import { servicePillHtml, isError, isWarning, isDebug, isImportantInfo } from '@/utils/service'
import { relTime } from '@/utils/time'
import type { EventEntry } from '@/types'

const metriq  = useMetriqStore()
const context = useContextStore()

const events = computed((): EventEntry[] =>
  metriq.events.filter(e =>
    (e.data.processType ?? '').toUpperCase() === 'INDEPENDENT' &&
    (e.data.brandName   ?? '').trim() === context.activeBrand
  )
)

function formatPayload(entry: EventEntry): string {
  const p = (entry.data as any).payload
  if (p == null) return ''
  return typeof p === 'string' ? p : JSON.stringify(p, null, 2)
}
</script>

<template>
  <main class="traces-main">
    <div v-if="events.length === 0" class="empty-state">
      <div class="empty-icon">⬡</div>
      <div class="empty-text">no independent events</div>
    </div>
    <div v-else class="flow-scroll">
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
            </div>
            <div class="node-row2">
              <div class="flow-node-code" v-if="entry.data.code">{{ entry.data.code }}</div>
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
  </main>
</template>

<style scoped>
.node-row1, .node-row2 { display: flex; align-items: center; gap: 8px; }
.node-type { font-family: var(--mono); font-size: 0.52rem; letter-spacing: 1px; color: var(--text-muted); }

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
