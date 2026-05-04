package com.semantyca.metriq.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.semantyca.core.repository.AsyncRepository;
import com.semantyca.core.repository.rls.RLSRepository;
import com.semantyca.metriq.dto.BrandSoundFragmentPlayDelta;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Tuple;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class BrandSoundFragmentRepository extends AsyncRepository {

    private static final String TABLE = "mixpla__brand_sound_fragments";

    public BrandSoundFragmentRepository() {
        super();
    }

    @Inject
    public BrandSoundFragmentRepository(PgPool client,
                                        ObjectMapper mapper,
                                        RLSRepository rlsRepository) {
        super(client, mapper, rlsRepository);
    }

    public Uni<Void> incrementPlayedCountsBatch(List<BrandSoundFragmentPlayDelta> deltas) {
        if (deltas.isEmpty()) {
            return Uni.createFrom().voidItem();
        }
        String sql = "INSERT INTO " + TABLE + " " +
                "(brand_id, sound_fragment_id, played_by_brand_count, last_time_played_by_brand) " +
                "VALUES ($1, $2, $3, NOW()) " +
                "ON CONFLICT (brand_id, sound_fragment_id) DO UPDATE SET " +
                "played_by_brand_count = " + TABLE + ".played_by_brand_count + EXCLUDED.played_by_brand_count, " +
                "last_time_played_by_brand = NOW()";
        List<Tuple> batch = new ArrayList<>(deltas.size());
        for (BrandSoundFragmentPlayDelta d : deltas) {
            batch.add(Tuple.of(d.brandId(), d.soundFragmentId(), d.count()));
        }
        return client.preparedQuery(sql).executeBatch(batch).replaceWithVoid();
    }
}
