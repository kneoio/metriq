<script setup lang="ts">
import { useAivoxStore } from '@/stores/aivox'
const aivox = useAivoxStore()
</script>

<template>
  <main class="aivox-main">
    <div class="status-card">
      <div class="sc-label">stream status</div>
      <div class="sc-row">
        <span class="sdot" :class="aivox.status"></span>
        <span class="sc-value">{{ aivox.status }}</span>
      </div>
      <div v-if="aivox.cmdStatus" class="sc-cmd">{{ aivox.cmdStatus }}</div>
    </div>
  </main>
</template>

<style scoped>
.aivox-main {
  flex: 1;
  padding: 36px 44px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  overflow-y: auto;
}

.status-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 22px 26px;
  border: 1px solid var(--border);
  border-radius: 6px;
  background: var(--surface);
  max-width: 360px;
}

.sc-label {
  font-family: var(--mono);
  font-size: 0.6rem;
  letter-spacing: 2.5px;
  color: var(--text-muted);
}

.sc-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.sdot {
  width: 6px; height: 6px; border-radius: 50%;
  background: var(--text-dim); flex-shrink: 0;
  transition: background 0.3s;
}
.sdot.running, .sdot.connected { background: var(--green); }
.sdot.error                    { background: var(--accent3); }

.sc-value {
  font-family: var(--mono);
  font-size: 0.8rem;
  color: var(--text);
}

.sc-cmd {
  font-family: var(--mono);
  font-size: 0.62rem;
  color: var(--text-muted);
}
</style>
