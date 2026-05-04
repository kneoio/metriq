package com.semantyca.metriq.service;

import com.semantyca.metriq.messaging.SongPlayedCountBuffer;
import com.semantyca.metriq.repository.BrandSoundFragmentRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

@ApplicationScoped
public class BrandSoundFragmentUpdateService {

    @Inject
    BrandSoundFragmentRepository brandSoundFragmentRepository;

    @Inject
    SongPlayedCountBuffer songPlayedCountBuffer;

    public void offerSongPlayed(UUID brandId, UUID soundFragmentId) {
        songPlayedCountBuffer.offer(brandId, soundFragmentId);
    }

    public Uni<Void> updatePlayedCountAsync(UUID brandId, UUID soundFragmentId) {
        return brandSoundFragmentRepository.incrementPlayedCount(brandId, soundFragmentId);
    }
}
