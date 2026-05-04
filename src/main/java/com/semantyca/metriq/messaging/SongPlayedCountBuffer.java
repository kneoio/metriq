package com.semantyca.metriq.messaging;

import com.semantyca.metriq.config.MetriqConfig;
import com.semantyca.metriq.dto.BrandSoundFragmentPlayDelta;
import com.semantyca.metriq.repository.BrandSoundFragmentRepository;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@ApplicationScoped
public class SongPlayedCountBuffer {
    private static final String JOB_ID = "song-played-flush";
    private static final Logger LOGGER = Logger.getLogger(SongPlayedCountBuffer.class);
    private volatile ConcurrentHashMap<Key, AtomicInteger> pending = new ConcurrentHashMap<>();

    @Inject
    MetriqConfig metriqConfig;

    @Inject
    Scheduler scheduler;

    @Inject
    BrandSoundFragmentRepository brandSoundFragmentRepository;

    public void offer(UUID brandId, UUID soundFragmentId) {
        pending.computeIfAbsent(new Key(brandId, soundFragmentId), k -> new AtomicInteger()).incrementAndGet();
    }

    void registerFlushJob(@Observes StartupEvent startup) {
        scheduler.newJob(JOB_ID)
                .setInterval(metriqConfig.songPlayedFlushInterval())
                .setConcurrentExecution(Scheduled.ConcurrentExecution.SKIP)
                .setTask(execution -> flush())
                .schedule();
    }

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
