package com.semantyca.metriq.service;

import com.semantyca.metriq.config.MetriqConfig;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.Cancellable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ApplicationScoped
public class LocalFileCleanupService {
    private static final Logger LOGGER = Logger.getLogger(LocalFileCleanupService.class);
    private static final Duration CLEANUP_INTERVAL = Duration.ofHours(1);
    private static final Duration INITIAL_DELAY = Duration.ofMinutes(10);

    private final List<String> folders;
    private final Duration maxAge;
    private Cancellable cleanupSubscription;

    private final AtomicLong filesDeleted = new AtomicLong(0);
    private final AtomicLong bytesFreed = new AtomicLong(0);
    private volatile LocalDateTime lastCleanupTime;

    @Inject
    public LocalFileCleanupService(MetriqConfig config) {
        this.folders = config.cleanup().folders().orElse(List.of());
        this.maxAge = Duration.ofHours(config.cleanup().maxAgeHours());
    }

    void onStart(@Observes StartupEvent event) {
        if (folders.isEmpty()) {
            LOGGER.info("LocalFileCleanupService: no folders configured, cleanup disabled");
            return;
        }
        LOGGER.infof("LocalFileCleanupService starting, watching: %s", folders);
        cleanupSubscription = Multi.createFrom().ticks()
                .startingAfter(INITIAL_DELAY)
                .every(CLEANUP_INTERVAL)
                .onOverflow().drop()
                .onItem().invoke(this::performCleanup)
                .onFailure().invoke(error -> LOGGER.error("Cleanup error", error))
                .subscribe().with(item -> {}, failure -> LOGGER.error("Cleanup subscription failed", failure));
    }

    public void stopCleanupTask() {
        if (cleanupSubscription != null) {
            cleanupSubscription.cancel();
        }
    }

    private void performCleanup(Long tick) {
        long startTime = System.currentTimeMillis();
        long deleted = 0;
        long freed = 0;

        for (String folder : folders) {
            Path dir = Paths.get(folder);
            if (!Files.exists(dir)) {
                LOGGER.debugf("Cleanup folder does not exist, skipping: %s", dir);
                continue;
            }
            Instant cutoff = Instant.now().minus(maxAge);
            try (Stream<Path> files = Files.walk(dir)) {
                for (Path file : files.filter(Files::isRegularFile).toArray(Path[]::new)) {
                    try {
                        if (Files.getLastModifiedTime(file).toInstant().isBefore(cutoff)) {
                            long size = Files.size(file);
                            Files.delete(file);
                            deleted++;
                            freed += size;
                            LOGGER.debugf("Deleted: %s", file);
                        }
                    } catch (Exception e) {
                        LOGGER.warnf(e, "Failed to delete file: %s", file);
                    }
                }
            } catch (Exception e) {
                LOGGER.errorf(e, "Failed to walk directory: %s", dir);
            }
        }

        filesDeleted.addAndGet(deleted);
        bytesFreed.addAndGet(freed);
        lastCleanupTime = LocalDateTime.now();

        long duration = System.currentTimeMillis() - startTime;
        double mbFreed = freed / (1024.0 * 1024);
        LOGGER.infof("Cleanup done in %dms — %d files deleted, %s MB freed", duration, deleted, String.format("%.2f", mbFreed));
    }

    public CleanupStats getStats() {
        return new CleanupStats(filesDeleted.get(), bytesFreed.get(), lastCleanupTime, folders);
    }

    public record CleanupStats(
            long filesDeleted,
            long bytesFreed,
            LocalDateTime lastCleanupTime,
            List<String> folders
    ) {}
}
