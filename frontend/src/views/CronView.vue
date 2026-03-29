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
    (e.data.processType ?? '').toUpperCase() === 'CRON' &&
    (e.data.brandName   ?? '').trim() === context.activeBrand
  )
)

function formatPayload(entry: EventEntry): string {
  const { _receivedAt, ...rest } = entry.data as any
  return JSON.stringify(rest, null, 2)
}

</script>

<template>
  <main class="process-main">
    <div v-if="events.length === 0" class="empty-state">
      <div class="empty-icon">⬡</div>
      <div class="empty-text">no cron events</div>
    </div>
    <div v-else class="event-list">
      <div v-for="entry in events" :key="entry.id" class="event-card"
        :class="{ 'is-error': isError(entry.data.type as string), 'is-debug': isDebug(entry.data.type as string) }">
        <div class="event-header">
          <div class="event-type"
            :style="isError(entry.data.type as string) ? 'color:var(--accent3)' : isWarning(entry.data.type as string) ? 'color:var(--amber)' : isDebug(entry.data.type as string) ? 'color:var(--text-dim)' : isImportantInfo(entry.data.type as string) ? 'color:var(--cyan)' : ''">
            {{ (entry.data.type || 'UNKNOWN').toUpperCase() }}
          </div>
          <span v-html="servicePillHtml(entry.data.serviceId as string)"></span>
          <div class="event-brand" v-if="entry.data.brandName">{{ entry.data.brandName }}</div>
          <div class="event-code"  v-if="entry.data.code">{{ entry.data.code }}</div>
          <div class="event-time">{{ relTime(entry.receivedAt) }}</div>
        </div>
        <div class="event-payload">
          <pre class="payload-pre">{{ formatPayload(entry) }}</pre>
        </div>
      </div>
    </div>
  </main>
</template>

<style scoped>
.process-main {
  flex: 1;
  padding: 20px 28px;
  overflow-y: auto;
}

.event-list {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 10px;
}

.event-card {
  border: 1px solid var(--border);
  border-radius: 5px;
  background: var(--surface);
  overflow: hidden;
  flex-shrink: 0;
  min-width: 260px;
  max-width: 600px;
  width: max-content;
}
.event-card.is-error { border-color: rgba(244,67,54,0.35); }
.event-card.is-debug { opacity: 0.6; }

.event-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  flex-wrap: wrap;
}

.event-type  { font-family: var(--mono); font-size: 0.7rem; font-weight: 600; letter-spacing: 1px; }
.event-brand { font-family: var(--mono); font-size: 0.65rem; color: var(--text-muted); }
.event-code  { font-family: var(--mono); font-size: 0.65rem; color: var(--amber); }
.event-time  { font-family: var(--mono); font-size: 0.6rem; color: var(--text-dim); margin-left: auto; }

.event-payload {
  border-top: 1px solid var(--border);
  padding: 10px 14px;
  background: rgba(0,0,0,0.15);
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
