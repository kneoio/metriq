package com.semantyca.metriq.service;

import com.semantyca.metriq.repository.SoundFragmentRepository;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.Cancellable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class SoundFragmentCleanupService {
    private static final Logger LOGGER = Logger.getLogger(SoundFragmentCleanupService.class);
    private static final Duration CLEANUP_INTERVAL = Duration.ofMinutes(15);
    private static final Duration INITIAL_DELAY = Duration.ofMinutes(5);

    private Cancellable cleanupSubscription;

    @Inject
    SoundFragmentService soundFragmentService;

    @Inject
    SoundFragmentRepository soundFragmentRepository;

    void onStart(@Observes StartupEvent event) {
        startCleanupTask();
    }

    private void startCleanupTask() {
        cleanupSubscription = Multi.createFrom().ticks()
                .startingAfter(INITIAL_DELAY)
                .every(CLEANUP_INTERVAL)
                .onOverflow().drop()
                .onItem().invoke(this::performCleanup)
                .onFailure().invoke(error -> LOGGER.error("SoundFragment cleanup error", error))
                .subscribe().with(
                        item -> {},
                        failure -> LOGGER.error("SoundFragment cleanup subscription failed", failure)
                );
    }

    public void stopCleanupTask() {
        if (cleanupSubscription != null) {
            cleanupSubscription.cancel();
        }
    }

    private void performCleanup(Long tick) {
        cleanupExpiredSoundFragments(tick)
                .subscribe().with(
                        count -> { if (count > 0) LOGGER.infof("SoundFragment cleanup (tick: %d) deleted %d expired SoundFragments", tick, count); },
                        failure -> LOGGER.errorf(failure, "Expired SoundFragment cleanup failed (tick: %d)", tick)
                );
        cleanupArchivedSoundFragments(tick)
                .subscribe().with(
                        count -> { if (count > 0) LOGGER.infof("SoundFragment cleanup (tick: %d) deleted %d archived SoundFragments", tick, count); },
                        failure -> LOGGER.errorf(failure, "Archived SoundFragment cleanup failed (tick: %d)", tick)
                );
    }

    private Uni<Integer> cleanupExpiredSoundFragments(Long tick) {
        return soundFragmentRepository.findExpiredFragments()
                .onItem().transformToUni(fragmentIds -> {
                    if (fragmentIds.isEmpty()) {
                        return Uni.createFrom().item(0);
                    }
                    List<Uni<Integer>> deleteOps = fragmentIds.stream()
                            .map(fragmentId -> soundFragmentService.hardDelete(fragmentId)
                                    .onFailure().recoverWithItem(error -> {
                                        LOGGER.errorf("Failed to delete expired SoundFragment %s: %s", fragmentId, error.getMessage(), error);
                                        return 0;
                                    }))
                            .toList();
                    return Uni.join().all(deleteOps).andFailFast()
                            .onItem().transform(results -> results.stream().mapToInt(Integer::intValue).sum());
                });
    }

    private Uni<Integer> cleanupArchivedSoundFragments(Long tick) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusMonths(1);
        return soundFragmentRepository.findArchivedFragments(cutoffDate)
                .onItem().transformToUni(fragmentIds -> {
                    if (fragmentIds.isEmpty()) {
                        return Uni.createFrom().item(0);
                    }
                    List<Uni<Integer>> deleteOps = fragmentIds.stream()
                            .map(fragmentId -> soundFragmentService.hardDelete(fragmentId)
                                    .onFailure().recoverWithItem(error -> {
                                        LOGGER.errorf("Failed to delete archived SoundFragment %s: %s", fragmentId, error.getMessage(), error);
                                        return 0;
                                    }))
                            .toList();
                    return Uni.join().all(deleteOps).andFailFast()
                            .onItem().transform(results -> results.stream().mapToInt(Integer::intValue).sum());
                });
    }
}
