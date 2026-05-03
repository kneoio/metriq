/**
 * Preset groups of metric `code` values for filtering Traces / Cron / Independent.
 */
export interface MetricEventCodeGroup {
  id: string
  label: string
  codes: readonly string[]
}

export const METRIC_EVENT_CODE_GROUPS: readonly MetricEventCodeGroup[] = [
  {
    id: 'queue_mixing',
    label: 'Queue / mixing (QueueService)',
    codes: [
      'mixing_begin',
      'intro_song_completed',
      'intro_song_failed',
      'not_mixed_completed',
      'not_mixed_failed',
      'song_intro_song_completed',
      'song_intro_song_failed',
      'intro_song_intro_song_completed',
      'intro_song_intro_song_failed',
      'song_crossfade_song_completed',
      'song_crossfade_song_failed',
      'song_only_completed',
      'song_only_failed',
      'filler_jingle_completed',
      'filler_jingle_failed',
      'jingle_generated_jingle_completed',
      'jingle_generated_jingle_failed',
      'intro_jingle_generated_jingle_completed',
      'intro_jingle_generated_jingle_failed',
      'unsupported_merging_method',
      'ots_mixing_begin',
    ],
  },
  {
    id: 'playlist',
    label: 'Playlist (PlaylistManager)',
    codes: ['song_added_to_regular_queue', 'song_added_to_prioritized_queue', 'adding_to_queue'],
  },
  {
    id: 'playback',
    label: 'Playback (Streamer)',
    codes: ['song_played'],
  },
  {
    id: 'intro_gain',
    label: 'Audio processing (IntroGainProcessor)',
    codes: ['intro_gain_applied'],
  },
  {
    id: 'rabbit_consumer',
    label: 'Rabbit queue consumer (QueueConsumer)',
    codes: ['radio_station_not_available', 'queue_request_failed'],
  },
  {
    id: 'files',
    label: 'Files (SoundFragmentFileHandler)',
    codes: ['file_key_non_ascii', 'file_retrieval_failed'],
  },
  {
    id: 'station_pool',
    label: 'Station pool (RadioStationPool)',
    codes: ['stream_initialized'],
  },
  {
    id: 'commands',
    label: 'Commands (CommandService)',
    codes: ['init_stream', 'stop_stream'],
  },
  {
    id: 'featured_stations',
    label: 'Featured stations (FeaturedStationManager)',
    codes: [
      'featured_station_fatal_failure',
      'featured_station_initialized',
      'featured_station_jesoos_retry',
      'featured_station_init_retry',
    ],
  },
  {
    id: 'inactivity_cron',
    label: 'Inactivity cron (StationInactivityChecker)',
    codes: [
      'station_inactivity_checker_started',
      'station_inactivity_checker_stopped',
      'station_removed',
      'dj_wakeup_sent',
      'station_offline',
      'jesoos_sleep_sent',
      'station_idle',
      'onetime_stream_finished',
      'station_activity_check_failed',
    ],
  },
  {
    id: 'flow_agenda_misc',
    label: 'Flow / agenda / auth / commands (event codes)',
    codes: [
      'scene_content_gap',
      'intro_tts_audio_generated',
      'intro_tts_audio_save_failed',
      'intro_tts_audio_generation_failed',
      'intro_spoken_text_generation_failed',
      'intro_spoken_text_generated',
      'silence_risk',
      'entry_emitted',
      'scene_started',
      'daily_agenda_rebuild_completed',
      'daily_agenda_rebuild_failed',
      'agenda_rebuilt',
      'logoff',
      'login_failed',
      'login_success',
      'command_emit_timeline_entry',
      'ots_entry_failed',
      'agenda_created',
      'station_stop',
      'entries_scheduled',
      'entry_failed',
      'scene_stopped',
    ],
  },
  {
    id: 'datanest',
    label: 'Datanest',
    codes: ['brand_closed'],
  },
] as const
