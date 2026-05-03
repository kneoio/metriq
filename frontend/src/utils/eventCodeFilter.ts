import type { EventEntry } from '@/types'

/**
 * When `filterText` is empty, every entry matches.
 * Otherwise split on comma or newline; entry matches if its `data.code` matches
 * any token (case-insensitive equality or substring). Entries with no code never match.
 */
export function eventEntryMatchesCodeFilter(entry: EventEntry, filterText: string): boolean {
  const raw = filterText.trim()
  if (!raw) return true
  const code = (entry.data.code ?? '').trim().toLowerCase()
  if (!code) return false
  const tokens = raw
    .split(/[,\n]+/)
    .map(t => t.trim())
    .filter(Boolean)
  if (tokens.length === 0) return true
  return tokens.some(tok => {
    const t = tok.toLowerCase()
    return code === t || code.includes(t)
  })
}
