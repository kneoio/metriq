package com.semantyca.metriq.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.semantyca.mixpla.dto.queue.metric.MetricEventDTO;
import com.semantyca.metriq.service.BrandSoundFragmentUpdateService;
import com.semantyca.metriq.store.EventStore;
import com.semantyca.metriq.ws.BrandMetricWebSocket;
import com.semantyca.metriq.ws.MetricWebSocket;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

import java.util.UUID;

@ApplicationScoped
public class MetricConsumer {

    private static final Logger LOGGER = Logger.getLogger(MetricConsumer.class);
    private static final String CODE_SONG_PLAYED = "song_played";
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Inject
    EventStore eventStore;

    @Inject
    MetricWebSocket metricWebSocket;

    @Inject
    BrandMetricWebSocket brandMetricWebSocket;

    @Inject
    BrandSoundFragmentUpdateService brandSoundFragmentUpdateService;

    @Incoming("metrics")
    public Uni<Void> consume(Message<byte[]> message) {
        byte[] payload = message.getPayload();

        return Uni.createFrom().item(() -> {
                    try {
                        String raw = new String(payload);

                        // Store first — before broadcasting — so any REST client
                        // connecting concurrently already sees the event.
                        eventStore.add(raw);

                        MetricEventDTO dto = parseDto(payload);
                        String brand = dto != null ? dto.brandName() : extractBrand(raw);
                        brandMetricWebSocket.broadcast(brand, raw);
                        metricWebSocket.broadcast(raw);

                        JsonObject evt = new JsonObject(raw);
                        if (CODE_SONG_PLAYED.equals(evt.getString("code"))) {
                            try {
                                JsonObject p = evt.getJsonObject("payload");
                                brandSoundFragmentUpdateService.offerSongPlayed(
                                        UUID.fromString(p.getString("brandId")),
                                        UUID.fromString(p.getString("songId")));
                            } catch (Exception ignored) {
                            }
                        }
                        return raw;
                    } catch (Exception e) {
                        LOGGER.error("Failed to deserialize metric event", e);
                        throw new RuntimeException(e);
                    }
                })
                .onItem().transformToUni(ignored ->
                        Uni.createFrom().completionStage(message.ack()))
                .onFailure().recoverWithUni(e -> {
                    LOGGER.error("Failed processing metric event", e);
                    return Uni.createFrom().completionStage(message.nack(e));
                });
    }

    private String extractBrand(String raw) {
        try {
            var root = objectMapper.readTree(raw);
            if (root.hasNonNull("brandName")) {
                return root.get("brandName").asText();
            }
            LOGGER.warn("brandName not found in metric payload");
        } catch (Exception e) {
            LOGGER.error("Failed to parse brand from metric payload", e);
        }
        return null;
    }

    private MetricEventDTO parseDto(byte[] payload) {
        try {
            return objectMapper.readValue(payload, MetricEventDTO.class);
        } catch (Exception e) {
            LOGGER.debug("Failed to deserialize MetricEventDTO; falling back to raw brand parsing", e);
            return null;
        }
    }
}
