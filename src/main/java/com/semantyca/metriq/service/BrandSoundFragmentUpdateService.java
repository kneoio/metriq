package com.semantyca.metriq.service;

import com.semantyca.metriq.messaging.SongPlayedCountBuffer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

@ApplicationScoped
public class BrandSoundFragmentUpdateService {

    @Inject
    SongPlayedCountBuffer songPlayedCountBuffer;

    public void offerSongPlayed(UUID brandId, UUID soundFragmentId) {
        songPlayedCountBuffer.offer(brandId, soundFragmentId);
    }
}
