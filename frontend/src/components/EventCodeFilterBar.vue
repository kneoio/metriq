<script setup lang="ts">
import { useTracesStore } from '@/stores/traces'
import { METRIC_EVENT_CODE_GROUPS } from '@/constants/metricEventCodes'

const traces = useTracesStore()

function clearFilter() {
  traces.eventCodeFilterGroupId = ''
}
</script>

<template>
  <div class="event-code-filter-bar">
    <label class="filter-label" for="event-code-group-select">code group</label>
    <div class="filter-row">
      <select
        id="event-code-group-select"
        class="preset-select"
        v-model="traces.eventCodeFilterGroupId"
      >
        <option value="">all codes</option>
        <option v-for="g in METRIC_EVENT_CODE_GROUPS" :key="g.id" :value="g.id">{{ g.label }}</option>
      </select>
      <button type="button" class="action-btn secondary" @click="clearFilter">clear</button>
    </div>
  </div>
</template>

<style scoped>
.event-code-filter-bar {
  width: 100%;
  padding: 10px 0 14px;
  border-bottom: 1px solid var(--border);
  margin-bottom: 4px;
}
.filter-label {
  display: block;
  font-family: var(--mono);
  font-size: 0.55rem;
  letter-spacing: 1.5px;
  color: var(--text-dim);
  text-transform: uppercase;
  margin-bottom: 6px;
}
.filter-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}
.preset-select {
  flex: 1 1 220px;
  min-width: 160px;
  max-width: 100%;
  font-family: var(--mono);
  font-size: 0.58rem;
  padding: 6px 10px;
  border-radius: 3px;
  border: 1px solid var(--border-bright);
  background: rgba(0, 0, 0, 0.25);
  color: var(--text-muted);
  cursor: pointer;
}
.preset-select:focus {
  outline: none;
  border-color: var(--accent);
}
</style>
