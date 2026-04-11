package com.semantyca.metriq.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.semantyca.core.repository.AsyncRepository;
import com.semantyca.core.repository.rls.RLSRepository;
import io.vertx.mutiny.pgclient.PgPool;

public abstract class SoundFragmentRepositoryAbstract extends AsyncRepository {
    public SoundFragmentRepositoryAbstract() {
        super();
    }

    public SoundFragmentRepositoryAbstract(PgPool client, ObjectMapper mapper, RLSRepository rlsRepository) {
        super(client, mapper, rlsRepository);
    }


}