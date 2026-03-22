<script setup lang="ts">
import { ref, reactive, watch, nextTick, onMounted, onUnmounted } from 'vue'
import gsap from 'gsap'
import { useMetriqStore } from '@/stores/metriq'
import { servicePillHtml, isError, isDebug } from '@/utils/service'
import { relTime, formatTs } from '@/utils/time'

const store = useMetriqStore()

// ── Refs ──────────────────────────────────────────────────────────────────────
const listEl      = ref<HTMLElement | null>(null)
const expandedIds = reactive(new Set<number>())
const payloadRefs: Record<number, HTMLElement> = {}
const timeTick    = ref(0)
let tickInterval: ReturnType<typeof setInterval> | null = null
let clearing = false

// ── GSAP number tweens ────────────────────────────────────────────────────────
const displayTotal  = ref(0)
const displayRate   = ref(0)
const displayErrors = ref(0)
const totalTarget  = { v: 0 }
const rateTarget   = { v: 0 }
const errorsTarget = { v: 0 }

watch(() => store.totalCount, v => gsap.to(totalTarget,  { v, duration: 0.4, ease: 'power2.out', onUpdate() { displayTotal.value  = Math.round(totalTarget.v)  } }))
watch(() => store.rateCount,  v => gsap.to(rateTarget,   { v, duration: 0.4, ease: 'power2.out', onUpdate() { displayRate.value   = Math.round(rateTarget.v)   } }))
watch(() => store.errorCount, v => gsap.to(errorsTarget, { v, duration: 0.4, ease: 'power2.out', onUpdate() { displayErrors.value = Math.round(errorsTarget.v) } }))

// ── Methods ───────────────────────────────────────────────────────────────────
function setPayloadRef(id: number, el: unknown) {
  if (el) payloadRefs[id] = el as HTMLElement
  else    delete payloadRefs[id]
}

function toggleCard(id: number) {
  const el = payloadRefs[id]
  if (!el) return
  if (expandedIds.has(id)) {
    gsap.to(el, { height: 0, duration: 0.3, ease: 'power2.inOut', onComplete() { expandedIds.delete(id) } })
  } else {
    expandedIds.add(id)
    nextTick(() => {
      const h = el.scrollHeight
      gsap.fromTo(el, { height: 0 }, { height: h, duration: 0.35, ease: 'power2.out', onComplete() { gsap.set(el, { height: 'auto' }) } })
      gsap.fromTo(el.querySelectorAll('.meta-pair,.payload-json-wrap'), { opacity: 0, y: 8 }, { opacity: 1, y: 0, duration: 0.25, stagger: 0.04 })
    })
  }
}

function clearAll() {
  clearing = true
  const cards = listEl.value?.querySelectorAll('.event-card') ?? []
  const reset = () => {
    store.clearAllEvents()
    expandedIds.clear()
    displayTotal.value = displayRate.value = displayErrors.value = 0
    totalTarget.v = rateTarget.v = errorsTarget.v = 0
    nextTick(() => { clearing = false })
  }
  if (!cards.length) { reset(); return }
  gsap.to(cards, { opacity: 0, y: 20, duration: 0.3, stagger: 0.02, onComplete: reset })
}

function onCardEnter(el: Element, done: () => void) {
  gsap.fromTo(el, { opacity: 0, y: -12 }, { opacity: 1, y: 0, duration: 0.35, ease: 'power3.out', onComplete: done })
}
function onCardLeave(el: Element, done: () => void) {
  if (clearing) { done(); return }
  gsap.to(el, { opacity: 0, y: 20, duration: 0.3, onComplete: done })
}
function onEmptyLeave(el: Element, done: () => void) {
  gsap.to(el, { opacity: 0, y: -8, duration: 0.2, onComplete: done })
}

function copyJson(btn: EventTarget | null, json: string) {
  const el = btn as HTMLButtonElement
  navigator.clipboard.writeText(json ?? '').then(() => {
    const orig = el.textContent ?? ''
    el.textContent = 'copied'
    setTimeout(() => { el.textContent = orig }, 1800)
  }).catch(() => { el.textContent = 'failed'; setTimeout(() => { el.textContent = 'copy' }, 1800) })
}

function relTimeReactive(date: Date) { void timeTick.value; return relTime(date) }

onMounted(() => { tickInterval = setInterval(() => { timeTick.value++ }, 20_000) })
onUnmounted(() => { if (tickInterval) clearInterval(tickInterval) })

// Expose to App.vue for sidebar stats + clear button
defineExpose({ displayTotal, displayRate, displayErrors, clearAll })
</script>

<template>
  <main class="main">
    <div class="events-list" ref="listEl">
      <transition :css="false" @leave="onEmptyLeave">
        <div v-if="store.filteredEvents.length === 0" class="empty-state">
          <div class="empty-icon">⬡</div>
          <div class="empty-text">awaiting events</div>
        </div>
      </transition>
      <transition-group :css="false" @enter="onCardEnter" @leave="onCardLeave">
        <div v-for="entry in store.filteredEvents" :key="entry.id"
          class="event-card" :class="{ expanded: expandedIds.has(entry.id) }">
          <div class="event-row" @click="toggleCard(entry.id)">
            <span class="brand-cell">{{ entry.data.brandName || '—' }}</span>
            <span class="type-cell"
              :style="isError(entry.data.type as string) ? 'color:var(--accent3);background:rgba(250,109,109,0.12);border-color:rgba(250,109,109,0.3)'
                    : isDebug(entry.data.type as string) ? 'color:var(--text-dim);background:rgba(176,176,176,0.06);border-color:rgba(176,176,176,0.15)'
                    : ''">
              {{ (entry.data.type || 'UNKNOWN').toUpperCase() }}</span>
            <span class="service-cell" v-html="servicePillHtml(entry.data.serviceId as string)"></span>
            <span class="code-cell">{{ entry.data.code || '—' }}</span>
            <span class="time-cell">{{ relTimeReactive(entry.receivedAt) }}</span>
            <span class="chevron-cell">›</span>
          </div>
          <div class="event-payload" :class="{ open: expandedIds.has(entry.id) }"
            :ref="(el: unknown) => setPayloadRef(entry.id, el)">
            <div class="payload-inner">
              <div class="payload-meta">
                <div class="meta-pair"><span class="meta-key">trace id</span><span class="meta-val">{{ entry.data.traceId || '—' }}</span></div>
                <div class="meta-pair"><span class="meta-key">code</span><span class="meta-val">{{ entry.data.code || '—' }}</span></div>
                <div class="meta-pair"><span class="meta-key">timestamp</span><span class="meta-val">{{ formatTs(entry.data.timestamp as string) }}</span></div>
                <div class="meta-pair"><span class="meta-key">received</span><span class="meta-val">{{ entry.receivedAt.toLocaleTimeString() }}</span></div>
              </div>
              <div class="payload-json-wrap">
                <pre class="payload-json">{{ JSON.stringify(entry.data.payload, null, 2) || 'null' }}</pre>
                <button class="copy-btn" @click.stop="copyJson($event.target, JSON.stringify(entry.data.payload, null, 2))">copy</button>
              </div>
            </div>
          </div>
        </div>
      </transition-group>
    </div>
  </main>
</template>
