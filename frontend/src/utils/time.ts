// ── Time utilities ────────────────────────────────────────────────────────────

export function relTime(date: Date | null | undefined): string {
  if (!date) return '—'
  const sec = Math.floor((Date.now() - date.getTime()) / 1000)
  if (sec < 10)  return 'just now'
  if (sec < 60)  return `${sec}s ago`
  const min = Math.floor(sec / 60)
  if (min < 60)  return `${min}m ago`
  const hr = Math.floor(min / 60)
  if (hr < 24)   return `${hr}h ago`
  return date.toLocaleDateString()
}

export function formatTs(ts: number | string | null | undefined): string {
  if (ts == null) return '—'
  return typeof ts === 'number' ? new Date(ts).toLocaleString() : (ts || '—')
}

/** Returns MM:SS delta between two EventEntry receivedAt dates. */
export function flowTimeDelta(
  prevMs: number | null | undefined,
  currMs: number | null | undefined,
): string {
  if (prevMs == null || currMs == null) return ''
  const ms = currMs - prevMs
  if (ms < 0) return '00:00'
  const totalSec = Math.floor(ms / 1000)
  const mm = String(Math.floor(totalSec / 60)).padStart(2, '0')
  const ss = String(totalSec % 60).padStart(2, '0')
  return `${mm}:${ss}`
}
