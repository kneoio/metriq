package com.semantyca.metriq.dto.playlist;

import java.util.UUID;

public class PlaylistQueueEntryDTO {
    public int pos;
    public String queueType;
    public UUID songId;
    public String title;
    public String artist;
    public Integer priority;
    /** {@code com.semantyca.mixpla.model.cnst.MixingType} name, e.g. {@code INTRO_SONG}. */
    public String mergingMethod;
}
