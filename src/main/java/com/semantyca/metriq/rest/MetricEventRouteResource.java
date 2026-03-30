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

@ApplicationScoped
public class MetricEventRouteResource {
    private static final Logger LOGGER = Logger.getLogger(MetricEventRouteResource.class);

    @Inject
    EventStore eventStore;

    public void setupRoutes(Router router) {
        String path = "/metriq";
        router.route(HttpMethod.GET, path + "/snapshot").handler(this::getSnapshot);
        router.route(HttpMethod.GET, path + "/events").handler(this::getEvents);
        router.route(HttpMethod.GET, path + "/events/:brand").handler(this::getEventsByBrand);
        router.route(HttpMethod.GET, path + "/traces").handler(this::getTraces);
        router.route(HttpMethod.GET, path + "/traces/:traceId").handler(this::getTrace);
        router.route(HttpMethod.DELETE, path + "/traces/:traceId").handler(this::deleteTrace);
        router.route(HttpMethod.GET, path + "/brands").handler(this::getBrands);
    }

    private void getSnapshot(RoutingContext rc) {
        try {
            JsonArray events = toArray(eventStore.getAll());

            JsonObject byBrand = new JsonObject();
            eventStore.getByBrand().forEach((brand, evts) -> byBrand.put(brand, toArray(evts)));

            JsonObject byTrace = new JsonObject();
            eventStore.getByTrace().forEach((traceId, evts) -> byTrace.put(traceId, toArray(evts)));

            JsonObject result = new JsonObject()
                    .put("events", events)
                    .put("byBrand", byBrand)
                    .put("byTrace", byTrace);

            rc.response()
                    .setStatusCode(200)
                    .putHeader("Content-Type", "application/json")
                    .end(result.encode());
        } catch (Exception e) {
            LOGGER.error("Failed to get snapshot", e);
            rc.response()
                    .setStatusCode(500)
                    .putHeader("Content-Type", "application/json")
                    .end(new JsonObject().put("error", e.getMessage()).encode());
        }
    }

    private void getEvents(RoutingContext rc) {
        try {
            JsonArray events = toArray(eventStore.getAll());
            rc.response()
                    .setStatusCode(200)
                    .putHeader("Content-Type", "application/json")
                    .end(events.encode());
        } catch (Exception e) {
            LOGGER.error("Failed to get events", e);
            rc.response()
                    .setStatusCode(500)
                    .putHeader("Content-Type", "application/json")
                    .end(new JsonObject().put("error", e.getMessage()).encode());
        }
    }

    private void getEventsByBrand(RoutingContext rc) {
        String brand = rc.pathParam("brand");
        try {
            JsonArray events = toArray(eventStore.getByBrand(brand));
            rc.response()
                    .setStatusCode(200)
                    .putHeader("Content-Type", "application/json")
                    .end(events.encode());
        } catch (Exception e) {
            LOGGER.error("Failed to get events for brand: " + brand, e);
            rc.response()
                    .setStatusCode(500)
                    .putHeader("Content-Type", "application/json")
                    .end(new JsonObject().put("error", e.getMessage()).encode());
        }
    }

    private void getTraces(RoutingContext rc) {
        try {
            JsonObject result = new JsonObject();
            eventStore.getByTrace().forEach((id, evts) -> result.put(id, toArray(evts)));
            rc.response()
                    .setStatusCode(200)
                    .putHeader("Content-Type", "application/json")
                    .end(result.encode());
        } catch (Exception e) {
            LOGGER.error("Failed to get traces", e);
            rc.response()
                    .setStatusCode(500)
                    .putHeader("Content-Type", "application/json")
                    .end(new JsonObject().put("error", e.getMessage()).encode());
        }
    }

    private void getTrace(RoutingContext rc) {
        String traceId = rc.pathParam("traceId");
        try {
            JsonArray events = toArray(eventStore.getByTrace(traceId));
            rc.response()
                    .setStatusCode(200)
                    .putHeader("Content-Type", "application/json")
                    .end(events.encode());
        } catch (Exception e) {
            LOGGER.error("Failed to get trace: " + traceId, e);
            rc.response()
                    .setStatusCode(500)
                    .putHeader("Content-Type", "application/json")
                    .end(new JsonObject().put("error", e.getMessage()).encode());
        }
    }

    private void deleteTrace(RoutingContext rc) {
        String traceId = rc.pathParam("traceId");
        try {
            boolean removed = eventStore.deleteTrace(traceId);
            if (removed) {
                rc.response()
                        .setStatusCode(204)
                        .end();
            } else {
                rc.response()
                        .setStatusCode(404)
                        .putHeader("Content-Type", "application/json")
                        .end(new JsonObject().put("error", "trace not found").encode());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to delete trace: " + traceId, e);
            rc.response()
                    .setStatusCode(500)
                    .putHeader("Content-Type", "application/json")
                    .end(new JsonObject().put("error", e.getMessage()).encode());
        }
    }

    private void getBrands(RoutingContext rc) {
        try {
            JsonArray arr = new JsonArray();
            eventStore.getKnownBrands().forEach(arr::add);
            rc.response()
                    .setStatusCode(200)
                    .putHeader("Content-Type", "application/json")
                    .end(arr.encode());
        } catch (Exception e) {
            LOGGER.error("Failed to get brands", e);
            rc.response()
                    .setStatusCode(500)
                    .putHeader("Content-Type", "application/json")
                    .end(new JsonObject().put("error", e.getMessage()).encode());
        }
    }

    private static JsonArray toArray(Iterable<JsonObject> items) {
        JsonArray arr = new JsonArray();
        items.forEach(arr::add);
        return arr;
    }
}
