/** Labels for `com.semantyca.mixpla.model.cnst.MixingType` (API field `mergingMethod`). */
export const MIXING_TYPE_LABELS: Record<string, string> = {
  INTRO_SONG: 'Intro → song',
  LISTENER_INTRO_SONG: 'Listener intro → song',
  NOT_MIXED: 'Not mixed',
  SONG_ONLY: 'Song only',
  SONG_INTRO_SONG: 'Song → intro → song',
  FILLER_JINGLE: 'Filler jingle',
  INTRO_SONG_INTRO_SONG: 'Intro → song → intro → song',
  SONG_CROSSFADE_SONG: 'Song crossfade',
  JINGLE_GENERATED_JINGLE_WITH_BACKGROUND: 'Generated jingle (with bed)',
  INTRO_JINGLE_GENERATED_JINGLE_WITH_BACKGROUND: 'Intro → generated jingle (with bed)',
}

const EM_DASH = '—'

export function mixingTypeLabel(value: string | null | undefined): string {
  if (value == null || value === '') return EM_DASH
  const label = MIXING_TYPE_LABELS[value]
  if (label !== undefined) return label
  return value
}
