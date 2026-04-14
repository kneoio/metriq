package com.semantyca.metriq.store;

import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class EventStore {

    private static final Logger LOGGER = Logger.getLogger(EventStore.class);

    private static final int  MAX_GLOBAL    = 120;
    private static final int  MAX_PER_BRAND = 500;
    private static final int  MAX_TRACES    = 200;
    private static final long MAX_AGE_MS    = 15 * 60 * 1_000L; // 15 min

    private final Deque<JsonObject> allEvents = new ArrayDeque<>(MAX_GLOBAL + 1);
    private final ConcurrentHashMap<String, Deque<JsonObject>> byBrand = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<JsonObject>> byTrace = new ConcurrentHashMap<>();
    private final Deque<String> traceOrder = new ArrayDeque<>();


    public synchronized void add(String raw) {
        try {
            JsonObject event = new JsonObject(raw);
            long now = System.currentTimeMillis();
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

    public synchronized boolean deleteTrace(String traceId) {
        boolean removed = byTrace.remove(traceId) != null;
        if (removed) traceOrder.remove(traceId);
        return removed;
    }

    public Set<String> getKnownBrands() {
        return byBrand.keySet();
    }

    public synchronized EvictionResult evictExpired() {
        long now = System.currentTimeMillis();
        int evictedBrandEvents = 0;
        int evictedTraces = 0;

        for (Deque<JsonObject> q : byBrand.values()) {
            int before = q.size();
            q.removeIf(e -> now - e.getLong("_receivedAt", 0L) > MAX_AGE_MS);
            evictedBrandEvents += before - q.size();
        }
        byBrand.entrySet().removeIf(e -> e.getValue().isEmpty());

        List<String> expiredTraces = new ArrayList<>();
        for (Map.Entry<String, List<JsonObject>> entry : byTrace.entrySet()) {
            entry.getValue().removeIf(e -> now - e.getLong("_receivedAt", 0L) > MAX_AGE_MS);
            if (entry.getValue().isEmpty()) {
                expiredTraces.add(entry.getKey());
            }
        }
        for (String traceId : expiredTraces) {
            byTrace.remove(traceId);
            traceOrder.remove(traceId);
            evictedTraces++;
        }

        return new EvictionResult(evictedBrandEvents, evictedTraces);
    }

    public record EvictionResult(int evictedBrandEvents, int evictedTraces) {}
}
