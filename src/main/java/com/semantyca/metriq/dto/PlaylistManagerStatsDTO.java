package com.semantyca.metriq.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PlaylistManagerStatsDTO {

    private List<LiveSoundFragmentDTO> livePlaylist;
    private List<LiveSoundFragmentDTO> queued;
    private List<LiveSoundFragmentDTO> playedSongs;
    private String brand;
    private int duration;



}
