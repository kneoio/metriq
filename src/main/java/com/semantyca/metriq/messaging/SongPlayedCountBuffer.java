package com.semantyca.metriq.messaging;

import com.semantyca.metriq.dto.BrandSoundFragmentPlayDelta;
import com.semantyca.metriq.repository.BrandSoundFragmentRepository;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@ApplicationScoped
public class SongPlayedCountBuffer {

    private static final Logger LOGGER = Logger.getLogger(SongPlayedCountBuffer.class);

    private volatile ConcurrentHashMap<Key, AtomicInteger> pending = new ConcurrentHashMap<>();

    @Inject
    BrandSoundFragmentRepository brandSoundFragmentRepository;

    public void offer(UUID brandId, UUID soundFragmentId) {
        pending.computeIfAbsent(new Key(brandId, soundFragmentId), k -> new AtomicInteger()).incrementAndGet();
    }

    @Scheduled(every = "${metriq.song-played-flush-interval:60s}")
    void flush() {
        if (pending.isEmpty()) {
            return;
        }
        ConcurrentHashMap<Key, AtomicInteger> snapshot = pending;
        pending = new ConcurrentHashMap<>();
        List<BrandSoundFragmentPlayDelta> deltas = new ArrayList<>();
        snapshot.forEach((k, count) -> {
            int n = count.get();
            if (n > 0) {
                deltas.add(new BrandSoundFragmentPlayDelta(k.brandId, k.soundFragmentId, n));
            }
        });
        if (deltas.isEmpty()) {
            return;
        }
        brandSoundFragmentRepository.incrementPlayedCountsBatch(deltas)
                .subscribe().with(
                        ignored -> {},
                        e -> LOGGER.errorf(e, "Batch flush of song_played counts failed (%d keys)", deltas.size()));
    }

    private record Key(UUID brandId, UUID soundFragmentId) {
    }
}
