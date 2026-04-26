package com.semantyca.metriq.service;

import com.semantyca.metriq.dto.playlist.PlaylistQueueStateDTO;
import com.semantyca.metriq.repository.PlaylistQueueRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class PlaylistQueueService {

    private final PlaylistQueueRepository repository;

    @Inject
    public PlaylistQueueService(PlaylistQueueRepository repository) {
        this.repository = repository;
    }

    public Uni<PlaylistQueueStateDTO> getQueueState(String brandSlug) {
        return repository.getQueueState(brandSlug);
    }
}
