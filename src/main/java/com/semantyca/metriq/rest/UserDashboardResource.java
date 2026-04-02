package com.semantyca.metriq.rest;

import com.semantyca.metriq.store.EventStore;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class UserDashboardResource {
    private static final Logger LOGGER = Logger.getLogger(UserDashboardResource.class);

    @Inject
    EventStore eventStore;

    public void setupRoutes(Router router) {
        String path = "/metriq";
        router.route(HttpMethod.GET, path + "/playlist/:brand").handler(this::getPlaylist);
    }

    private void getPlaylist(RoutingContext rc) {
        try {
            String brand = rc.pathParam("brand");
            List<JsonObject> events = eventStore.getByBrand(brand);

            List<JsonObject> history = new ArrayList<>();
            JsonArray upcomingPrioritized = new JsonArray();
            JsonArray upcomingRegular     = new JsonArray();

            for (JsonObject event : events) {
                String code = event.getString("code", "");

                if ("now_playing".equals(code)) {
                    JsonObject payload = event.getJsonObject("payload");
                    if (payload == null) continue;
                    history.stream()
                            .filter(s -> "playing".equals(s.getString("status")))
                            .forEach(s -> s.put("status", "played"));
                    history.add(new JsonObject()
                            .put("songId",     payload.getString("songId", ""))
                            .put("title",      payload.getString("title", ""))
                            .put("artist",     payload.getString("artist", ""))
                            .put("duration",   payload.getInteger("duration", 0))
                            .put("status",     "playing")
                            .put("receivedAt", event.getLong("_receivedAt", 0L)));

                } else if ("song_ended".equals(code)) {
                    history.stream()
                            .filter(s -> "playing".equals(s.getString("status")))
                            .forEach(s -> s.put("status", "played"));

                } else if ("queue_updated".equals(code)) {
                    JsonObject payload = event.getJsonObject("payload");
                    if (payload == null) continue;
                    JsonArray p = payload.getJsonArray("prioritizedQueueSongs");
                    JsonArray r = payload.getJsonArray("regularQueueSongs");
                    upcomingPrioritized = p != null ? p : new JsonArray();
                    upcomingRegular     = r != null ? r : new JsonArray();
                }
            }

            // Build result: history (played/playing) + upcoming queue
            JsonArray result = new JsonArray();
            history.forEach(result::add);

            for (int i = 0; i < upcomingPrioritized.size(); i++) {
                JsonObject s = upcomingPrioritized.getJsonObject(i);
                result.add(new JsonObject()
                        .put("songId",   s.getString("songId", ""))
                        .put("title",    s.getString("title", ""))
                        .put("artist",   s.getString("artist", ""))
                        .put("duration", s.getInteger("duration", 0))
                        .put("status",   "queued")
                        .put("queue",    "priority"));
            }
            for (int i = 0; i < upcomingRegular.size(); i++) {
                JsonObject s = upcomingRegular.getJsonObject(i);
                result.add(new JsonObject()
                        .put("songId",   s.getString("songId", ""))
                        .put("title",    s.getString("title", ""))
                        .put("artist",   s.getString("artist", ""))
                        .put("duration", s.getInteger("duration", 0))
                        .put("status",   "queued")
                        .put("queue",    "regular"));
            }

            rc.response()
                    .setStatusCode(200)
                    .putHeader("Content-Type", "application/json")
                    .end(result.encode());
        } catch (Exception e) {
            LOGGER.error("Failed to get playlist", e);
            rc.response()
                    .setStatusCode(500)
                    .putHeader("Content-Type", "application/json")
                    .end(new JsonObject().put("error", e.getMessage()).encode());
        }
    }
}
