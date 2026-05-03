import type { EventEntry } from '@/types'
import { METRIC_EVENT_CODE_GROUPS } from '@/constants/metricEventCodes'

const codesByGroupId = new Map<string, Set<string>>()
for (const g of METRIC_EVENT_CODE_GROUPS) {
  codesByGroupId.set(g.id, new Set(g.codes.map(c => c.toLowerCase())))
}

/**
 * Empty `groupId`: no filter. Otherwise the event matches only if `data.code`
 * is one of the known codes for that preset group (case-insensitive exact match).
 */
export function eventEntryMatchesCodeGroupFilter(entry: EventEntry, groupId: string): boolean {
  const id = groupId.trim()
  if (!id) return true
  const allowed = codesByGroupId.get(id)
  if (!allowed?.size) return true
  const code = (entry.data.code ?? '').trim().toLowerCase()
  if (!code) return false
  return allowed.has(code)
}
