package com.semantyca.metriq.repository;

import com.semantyca.metriq.dto.playlist.PlaylistQueueEntryDTO;
import com.semantyca.metriq.dto.playlist.PlaylistQueueStateDTO;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonArray;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.Tuple;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class PlaylistQueueRepository {
    private static final Logger LOGGER = Logger.getLogger(PlaylistQueueRepository.class);

    @Inject
    PgPool client;

    public Uni<PlaylistQueueStateDTO> getQueueState(String brandSlug) {
        String sql = "SELECT brand_id, full_queue, updated_at FROM mixpla__playlist_queue_state WHERE brand_slug = $1";

        return client.preparedQuery(sql)
                .execute(Tuple.of(brandSlug))
                .onItem().transform(rows -> {
                    if (rows.size() == 0) return null;
                    Row row = rows.iterator().next();
                    PlaylistQueueStateDTO dto = new PlaylistQueueStateDTO();
                    dto.brandId = row.getUUID("brand_id");
                    dto.fullQueue = deserializeQueue(row.getJsonArray("full_queue"));
                    dto.updatedAt = row.getOffsetDateTime("updated_at");
                    return dto;
                })
                .onFailure().invoke(e -> LOGGER.errorf(e, "[%s] Failed to load queue state", brandSlug));
    }

    private List<PlaylistQueueEntryDTO> deserializeQueue(JsonArray jsonArray) {
        if (jsonArray == null) return Collections.emptyList();

        List<PlaylistQueueEntryDTO> result = new ArrayList<>();
        for (int i = 0; i < jsonArray.size(); i++) {
            try {
                io.vertx.core.json.JsonObject entry = jsonArray.getJsonObject(i);
                PlaylistQueueEntryDTO dto = new PlaylistQueueEntryDTO();
                dto.pos = entry.getInteger("pos", i);
                dto.queueType = entry.getString("queueType");
                Integer rawPriority = entry.getInteger("priority");
                dto.priority = (rawPriority == null || rawPriority <= 0) ? 10 : rawPriority;
                String songIdStr = entry.getString("songId");
                dto.songId = songIdStr != null ? UUID.fromString(songIdStr) : null;
                dto.title = entry.getString("title");
                dto.artist = entry.getString("artist");
                result.add(dto);
            } catch (Exception e) {
                LOGGER.warnf("Failed to deserialize queue entry at index %d: %s", i, e.getMessage());
            }
        }
        return result;
    }
}
