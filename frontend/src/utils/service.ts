// ── Service utilities ─────────────────────────────────────────────────────────

const SERVICE_CLASS: Record<string, string> = {
  aivox:    'svc-aivox',
  jesoos:   'svc-jesoos',
  datanest: 'svc-datanest',
  metriq:   'svc-metriq',
  nivaro:   'svc-nivaro',
}

export function svcClass(id: string | undefined | null): string {
  return SERVICE_CLASS[(id || '').toLowerCase()] ?? 'svc-unknown'
}

function escHtml(str: unknown): string {
  if (str == null) return ''
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

export function servicePillHtml(id: string | undefined | null): string {
  if (!id) return '<span class="service-pill svc-unknown">—</span>'
  return `<span class="service-pill ${svcClass(id)}">${escHtml(id)}</span>`
}

export function isError(type: string | undefined | null): boolean {
  const t = (type ?? '').toUpperCase()
  return t.includes('ERROR') || t.includes('FAIL')
}

export function isDebug(type: string | undefined | null): boolean {
  return (type ?? '').toUpperCase().includes('DEBUG')
}

export interface ServiceOption {
  value: string
  label: string
  cls:   string
}

export const SERVICE_OPTIONS: ServiceOption[] = [
  { value: 'all',      label: 'all',      cls: '' },
  { value: 'aivox',    label: 'aivox',    cls: 'svc-aivox'    },
  { value: 'jesoos',   label: 'jesoos',   cls: 'svc-jesoos'   },
  { value: 'datanest', label: 'datanest', cls: 'svc-datanest' },
  { value: 'metriq',   label: 'metriq',   cls: 'svc-metriq'   },
  { value: 'nivaro',   label: 'nivaro',   cls: 'svc-nivaro'   },
]

export const STATION_LIST: string[] = [
  'lumisonic', 'sacana', 'em07chat', 'malucra',
  'bratan', 'sunonation', 'mixplaclone', 'aye-ayes-ear',
]
