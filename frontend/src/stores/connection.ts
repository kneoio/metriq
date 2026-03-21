import { ref } from 'vue'
import { defineStore } from 'pinia'
import type { ConnectionStatus } from '@/types'
import { useMetriqStore } from './metriq'

export const useConnectionStore = defineStore('connection', () => {
  // ── State ──────────────────────────────────────────────────────────────────
  const status = ref<ConnectionStatus>('connecting')

  let ws: WebSocket | null = null
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null

  // ── Connect ────────────────────────────────────────────────────────────────
  function connect() {
    const metriq = useMetriqStore()
    const scheme = window.location.protocol === 'https:' ? 'wss' : 'ws'
    status.value = 'connecting'

    ws = new WebSocket(`${scheme}://${window.location.host}/metriq/ws/metrics`)

    ws.onopen = () => {
      status.value = 'connected'
    }

    ws.onmessage = (msg: MessageEvent) => {
      try {
        metriq.ingestEvent(JSON.parse(msg.data as string))
      } catch (e) {
        console.error('[metriq ws] parse error', e)
      }
    }

    ws.onclose = () => {
      status.value = 'disconnected'
      reconnectTimer = setTimeout(connect, 3000)
    }

    ws.onerror = () => {
      // onclose fires immediately after, so no extra handling needed
    }
  }

  // ── Reconnect (manual) ────────────────────────────────────────────────────
  function reconnect() {
    if (reconnectTimer) { clearTimeout(reconnectTimer); reconnectTimer = null }
    if (ws) { ws.onclose = null; ws.close(); ws = null }
    connect()
  }

  // ── Disconnect ─────────────────────────────────────────────────────────────
  function disconnect() {
    if (reconnectTimer) { clearTimeout(reconnectTimer); reconnectTimer = null }
    if (ws) { ws.onclose = null; ws.close(); ws = null }
    status.value = 'disconnected'
  }

  return { status, connect, reconnect, disconnect }
})
