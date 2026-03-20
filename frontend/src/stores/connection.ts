import { ref } from 'vue'
import { defineStore } from 'pinia'
import type { ConnectionStatus } from '@/types'
import { useMetriqStore } from './metriq'

export const useConnectionStore = defineStore('connection', () => {
  // ── State ──────────────────────────────────────────────────────────────────
  const status  = ref<ConnectionStatus>('connecting')
  const wsHost  = ref<string>(
    localStorage.getItem('metriq_ws_host') ?? window.location.host
  )

  let ws: WebSocket | null = null
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null

  // ── Connect ────────────────────────────────────────────────────────────────
  function connect() {
    const metriq = useMetriqStore()
    const host   = wsHost.value.trim() || window.location.host
    status.value = 'connecting'

    ws = new WebSocket(`ws://${host}/metriq/ws/metrics`)

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
  function reconnect(newHost?: string) {
    if (newHost !== undefined) wsHost.value = newHost
    const host = wsHost.value.trim() || window.location.host
    localStorage.setItem('metriq_ws_host', host)

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

  return { status, wsHost, connect, reconnect, disconnect }
})
