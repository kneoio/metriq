package com.semantyca.metriq.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.semantyca.metriq.websocket.MetricWebSocket;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

@ApplicationScoped
public class MetricConsumer {

    private static final Logger LOGGER = Logger.getLogger(MetricConsumer.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    MetricWebSocket metricWebSocket;

    @Incoming("metrics")
    public Uni<Void> consume(Message<byte[]> message) {
        byte[] payload = message.getPayload();

        return Uni.createFrom().item(() -> {
                    try {
                        String raw = new String(payload);
                        LOGGER.info("Received metric event: " + raw);
                        metricWebSocket.broadcast(raw);
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
