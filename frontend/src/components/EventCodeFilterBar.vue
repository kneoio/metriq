<script setup lang="ts">
import { ref, computed } from 'vue'
import { useTracesStore } from '@/stores/traces'
import {
  METRIC_EVENT_CODE_GROUPS,
  METRIC_EVENT_CODE_CATALOG_SORTED,
  METRIC_EVENT_CODE_CATALOG_TEXT,
} from '@/constants/metricEventCodes'

const props = defineProps<{
  /** Unique `code` values currently visible (after filter), for “copy visible codes”. */
  visibleCodes: readonly string[]
}>()

const traces = useTracesStore()

const presetId = ref('')
const catalogCopyLabel = ref('COPY CATALOG')
const visibleCopyLabel = ref('COPY VISIBLE CODES')

const datalistId = `metric-code-datalist-${Math.random().toString(36).slice(2, 9)}`

const visibleCodesText = computed(() => [...new Set(props.visibleCodes)].sort((a, b) => a.localeCompare(b)).join('\n'))

function onPresetChange() {
  const id = presetId.value
  if (!id) {
    traces.eventCodeFilterText = ''
    return
  }
  const g = METRIC_EVENT_CODE_GROUPS.find(x => x.id === id)
  traces.eventCodeFilterText = g ? [...g.codes].join(',') : ''
}

function clearFilter() {
  traces.eventCodeFilterText = ''
  presetId.value = ''
}

function copyCatalog() {
  navigator.clipboard.writeText(METRIC_EVENT_CODE_CATALOG_TEXT).then(() => {
    catalogCopyLabel.value = 'COPIED'
    setTimeout(() => { catalogCopyLabel.value = 'COPY CATALOG' }, 1600)
  })
}

function copyVisibleCodes() {
  const t = visibleCodesText.value
  if (!t) return
  navigator.clipboard.writeText(t).then(() => {
    visibleCopyLabel.value = 'COPIED'
    setTimeout(() => { visibleCopyLabel.value = 'COPY VISIBLE CODES' }, 1600)
  })
}
</script>

<template>
  <div class="event-code-filter-bar">
    <label class="filter-label" for="event-code-filter-input">code filter</label>
    <div class="filter-row">
      <input
        id="event-code-filter-input"
        class="filter-input"
        type="text"
        v-model="traces.eventCodeFilterText"
        :list="datalistId"
        placeholder="comma or newline — substring match, OR across tokens"
        spellcheck="false"
        autocomplete="off"
      />
      <datalist :id="datalistId">
        <option v-for="c in METRIC_EVENT_CODE_CATALOG_SORTED" :key="c" :value="c" />
      </datalist>
      <select class="preset-select" v-model="presetId" @change="onPresetChange">
        <option value="">preset: all</option>
        <option v-for="g in METRIC_EVENT_CODE_GROUPS" :key="g.id" :value="g.id">{{ g.label }}</option>
      </select>
      <button type="button" class="action-btn secondary" @click="clearFilter">clear</button>
      <button type="button" class="action-btn secondary" @click="copyCatalog">{{ catalogCopyLabel }}</button>
      <button type="button" class="action-btn secondary" :disabled="!visibleCodesText" @click="copyVisibleCodes">
        {{ visibleCopyLabel }}
      </button>
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
.filter-input {
  flex: 1 1 180px;
  min-width: 140px;
  font-family: var(--mono);
  font-size: 0.62rem;
  padding: 6px 10px;
  border-radius: 3px;
  border: 1px solid var(--border-bright);
  background: rgba(0, 0, 0, 0.25);
  color: var(--text-muted);
}
.filter-input::placeholder {
  color: var(--text-dim);
  font-size: 0.58rem;
}
.filter-input:focus {
  outline: none;
  border-color: var(--accent);
  color: var(--text);
}
.preset-select {
  font-family: var(--mono);
  font-size: 0.58rem;
  max-width: min(280px, 100%);
  padding: 5px 8px;
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
