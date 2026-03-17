package com.semantyca.metriq.ws;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import org.jboss.logging.Logger;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/metriq/ws/metrics")
@ApplicationScoped
public class MetricWebSocket {

    private static final Logger LOGGER = Logger.getLogger(MetricWebSocket.class);
    private final Set<Session> sessions = ConcurrentHashMap.newKeySet();

    @OnOpen
    public void onOpen(Session session) {
        sessions.add(session);
        LOGGER.info("WebSocket connected: " + session.getId() + ", total sessions: " + sessions.size());
    }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(session);
        LOGGER.info("WebSocket disconnected: " + session.getId() + ", total sessions: " + sessions.size());
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        sessions.remove(session);
        LOGGER.error("WebSocket error for session: " + session.getId(), throwable);
    }

    public void broadcast(String message) {
        sessions.forEach(session -> {
            if (session.isOpen()) {
                session.getAsyncRemote().sendText(message, result -> {
                    if (result.getException() != null) {
                        LOGGER.error("Failed to send message to session: " + session.getId(), result.getException());
                    }
                });
            }
        });
    }
}
