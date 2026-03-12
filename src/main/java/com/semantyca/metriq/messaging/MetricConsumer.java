package com.semantyca.metriq.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

@ApplicationScoped
public class MetricConsumer {

    private static final Logger LOGGER = Logger.getLogger(MetricConsumer.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Incoming("metrics")
    public Uni<Void> consume(Message<byte[]> message) {
        byte[] payload = message.getPayload();

        return Uni.createFrom().item(() -> {
                    try {
                        // TODO: deserialize to MetricEventDTO once defined
                        String raw = new String(payload);
                        LOGGER.info("Received metric event: " + raw);
                        return raw;
                    } catch (Exception e) {
                        LOGGER.error("Failed to deserialize metric event", e);
                        throw new RuntimeException(e);
                    }
                })
                .onItem().transformToUni(raw ->
                        Uni.createFrom().completionStage(message.ack()))
                .onFailure().recoverWithUni(e -> {
                    LOGGER.error("Failed processing metric event", e);
                    return Uni.createFrom().completionStage(message.nack(e));
                });
    }
}