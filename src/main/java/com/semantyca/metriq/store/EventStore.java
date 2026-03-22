package com.semantyca.metriq.store;

import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store for metric events.
 *
 * Mirrors the three structures the frontend Pinia store used to maintain,
 * but now lives on the backend so any client (metriq FE, Knox dashboard, …)
 * can fetch a consistent snapshot via REST and then follow live updates over
 * the WebSocket.
 *
 * Caps (same as the old FE limits):
 *   - allEvents  : 120 items, 15-minute rolling window
 *   - perBrand   : 500 events per brand
 *   - byTrace    : 200 distinct trace IDs
 */
@ApplicationScoped
public class EventStore {

    private static final Logger LOGGER = Logger.getLogger(EventStore.class);

    private static final int  MAX_GLOBAL    = 120;
    private static final int  MAX_PER_BRAND = 500;
    private static final int  MAX_TRACES    = 200;
    private static final long MAX_AGE_MS    = 15 * 60 * 1_000L; // 15 min

    // Global rolling window (newest first)
    private final Deque<JsonObject> allEvents = new ArrayDeque<>(MAX_GLOBAL + 1);

    // Per-brand event queues (oldest first within each brand)
    private final ConcurrentHashMap<String, Deque<JsonObject>> byBrand = new ConcurrentHashMap<>();

    // Per-trace event lists (oldest first within each trace)
    private final ConcurrentHashMap<String, List<JsonObject>> byTrace = new ConcurrentHashMap<>();

    // Ordered traceId insertion log — used to evict oldest trace when cap is hit
    private final Deque<String> traceOrder = new ArrayDeque<>();

    // ── Public API ────────────────────────────────────────────────────────────

    public synchronized void add(String raw) {
        try {
            JsonObject event = new JsonObject(raw);
            long now = System.currentTimeMillis();
            // Tag with server-side receive time so clients can restore order
            event.put("_receivedAt", now);

            // ── global ──
            allEvents.addFirst(event);
            if (allEvents.size() > MAX_GLOBAL) allEvents.removeLast();
            allEvents.removeIf(e -> now - e.getLong("_receivedAt", 0L) > MAX_AGE_MS);

            // ── by brand ──
            String brand = event.getString("brandName", "").trim();
            if (!brand.isEmpty()) {
                Deque<JsonObject> q = byBrand.computeIfAbsent(brand, k -> new ArrayDeque<>());
                q.addLast(event);
                if (q.size() > MAX_PER_BRAND) q.removeFirst();
            }

            // ── by trace ──
            Object traceRaw = event.getValue("traceId");
            String traceId  = traceRaw != null ? String.valueOf(traceRaw).trim() : "";
            if (!traceId.isEmpty() && !traceId.equals("null")) {
                if (!byTrace.containsKey(traceId)) {
                    if (byTrace.size() >= MAX_TRACES && !traceOrder.isEmpty()) {
                        byTrace.remove(traceOrder.pollFirst());
                    }
                    byTrace.put(traceId, new ArrayList<>());
                    traceOrder.addLast(traceId);
                }
                byTrace.get(traceId).add(event);
            }

        } catch (Exception e) {
            LOGGER.warnf("EventStore.add failed: %s", e.getMessage());
        }
    }

    public synchronized List<JsonObject> getAll() {
        return new ArrayList<>(allEvents);
    }

    public synchronized Map<String, List<JsonObject>> getByBrand() {
        Map<String, List<JsonObject>> result = new LinkedHashMap<>();
        byBrand.forEach((brand, deque) -> result.put(brand, new ArrayList<>(deque)));
        return result;
    }

    public synchronized List<JsonObject> getByBrand(String brand) {
        Deque<JsonObject> q = byBrand.get(brand);
        return q == null ? Collections.emptyList() : new ArrayList<>(q);
    }

    public synchronized Map<String, List<JsonObject>> getByTrace() {
        Map<String, List<JsonObject>> result = new LinkedHashMap<>();
        byTrace.forEach((id, list) -> result.put(id, new ArrayList<>(list)));
        return result;
    }

    public synchronized List<JsonObject> getByTrace(String traceId) {
        return new ArrayList<>(byTrace.getOrDefault(traceId, Collections.emptyList()));
    }

    public Set<String> getKnownBrands() {
        return byBrand.keySet();
    }
}
