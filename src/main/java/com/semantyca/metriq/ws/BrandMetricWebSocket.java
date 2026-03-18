package com.semantyca.metriq.ws;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.jboss.logging.Logger;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/metriq/ws/metrics/{brand}")
@ApplicationScoped
public class BrandMetricWebSocket {

    private static final Logger LOGGER = Logger.getLogger(BrandMetricWebSocket.class);

    private final ConcurrentHashMap<String, Set<Session>> brandSessions = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("brand") String brand) {
        Set<Session> sessions = brandSessions.computeIfAbsent(brand, key -> ConcurrentHashMap.newKeySet());
        sessions.add(session);
        LOGGER.infof("WebSocket connected: %s for brand: %s, total sessions for brand: %d", session.getId(), brand, sessions.size());
    }

    @OnClose
    public void onClose(Session session, @PathParam("brand") String brand) {
        removeSession(brand, session);
    }

    @OnError
    public void onError(Session session, Throwable throwable, @PathParam("brand") String brand) {
        removeSession(brand, session);
        LOGGER.error("WebSocket error for session: " + session.getId(), throwable);
    }

    public void broadcast(String brand, String message) {
        Set<Session> sessions = brandSessions.get(brand);
        if (sessions == null) {
            LOGGER.debugf("No sessions registered for brand: %s", brand);
            return;
        }

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

    private void removeSession(String brand, Session session) {
        Set<Session> sessions = brandSessions.get(brand);
        if (sessions == null) {
            return;
        }

        sessions.remove(session);
        if (sessions.isEmpty()) {
            brandSessions.remove(brand);
        }
        LOGGER.infof("WebSocket disconnected: %s for brand: %s, remaining sessions for brand: %d", session.getId(), brand, sessions.size());
    }
}
