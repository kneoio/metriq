package com.semantyca.metriq.rest;

import com.semantyca.metriq.store.EventStore;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST API for reading the in-memory EventStore.
 *
 * Primary use-case: clients (metriq FE, Knox dashboard) call
 * GET /metriq/api/snapshot once on connect to seed their local cache,
 * then follow live updates over the WebSocket.
 */
@Path("/metriq/api")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
public class MetricEventResource {

    @Inject
    EventStore eventStore;

    /**
     * Full snapshot — one call to seed the entire client state.
     * Returns: { events: [...], byBrand: { brand: [...] }, byTrace: { id: [...] } }
     */
    @GET
    @Path("/snapshot")
    public Response getSnapshot() {
        JsonArray events = toArray(eventStore.getAll());

        JsonObject byBrand = new JsonObject();
        eventStore.getByBrand().forEach((brand, evts) -> byBrand.put(brand, toArray(evts)));

        JsonObject byTrace = new JsonObject();
        eventStore.getByTrace().forEach((traceId, evts) -> byTrace.put(traceId, toArray(evts)));

        return json(new JsonObject()
                .put("events", events)
                .put("byBrand", byBrand)
                .put("byTrace", byTrace));
    }

    /** Last 120 events (global, newest first). */
    @GET
    @Path("/events")
    public Response getEvents() {
        return json(toArray(eventStore.getAll()));
    }

    /** Events for a specific brand (up to 500, oldest first). */
    @GET
    @Path("/events/{brand}")
    public Response getEventsByBrand(@PathParam("brand") String brand) {
        return json(toArray(eventStore.getByBrand(brand)));
    }

    /** All traces as a map of traceId → event list. */
    @GET
    @Path("/traces")
    public Response getTraces() {
        JsonObject result = new JsonObject();
        eventStore.getByTrace().forEach((id, evts) -> result.put(id, toArray(evts)));
        return json(result);
    }

    /** Events for one specific trace. */
    @GET
    @Path("/traces/{traceId}")
    public Response getTrace(@PathParam("traceId") String traceId) {
        return json(toArray(eventStore.getByTrace(traceId)));
    }

    /** Known brand names. */
    @GET
    @Path("/brands")
    public Response getBrands() {
        JsonArray arr = new JsonArray();
        eventStore.getKnownBrands().forEach(arr::add);
        return json(arr);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static JsonArray toArray(Iterable<JsonObject> items) {
        JsonArray arr = new JsonArray();
        items.forEach(arr::add);
        return arr;
    }

    private static Response json(Object body) {
        String encoded = body instanceof JsonObject jo ? jo.encode()
                       : body instanceof JsonArray  ja ? ja.encode()
                       : body.toString();
        return Response.ok(encoded).type(MediaType.APPLICATION_JSON).build();
    }
}
