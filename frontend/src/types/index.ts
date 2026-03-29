// ── Domain types ─────────────────────────────────────────────────────────────

export interface MetricEventData {
  type?:        string
  processType?: string
  brandName?:   string
  serviceId?:   string
  traceId?:     string | number
  code?:        string
  payload?:     unknown
  timestamp?:   number | string
  [key: string]: unknown
}

export interface EventEntry {
  id:         number
  data:       MetricEventData
  receivedAt: Date
}

export type ConnectionStatus = 'connecting' | 'connected' | 'disconnected'

// ── Player types ──────────────────────────────────────────────────────────────

export interface PlayerLogLine {
  ts:   string
  msg:  string
  type: 'info' | 'ok' | 'warn' | 'error' | 'event'
}

// ── Jesoos types ──────────────────────────────────────────────────────────────

export interface JesoosScene {
  time?:     string
  type?:     string
  title?:    string
  artist?:   string
  duration?: number
  songs?:    unknown[]
  [key: string]: unknown
}

export interface JesoosAgenda {
  brand?:  string
  scenes?: JesoosScene[]
  total?:  number
  [key: string]: unknown
}
