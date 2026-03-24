package com.semantyca.metriq.rest;

import com.semantyca.metriq.store.EventStore;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/metriq/api")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
public class MetricEventResource {

    @Inject
    EventStore eventStore;

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

    @GET
    @Path("/events")
    public Response getEvents() {
        return json(toArray(eventStore.getAll()));
    }

    @GET
    @Path("/events/{brand}")
    public Response getEventsByBrand(@PathParam("brand") String brand) {
        return json(toArray(eventStore.getByBrand(brand)));
    }

    @GET
    @Path("/traces")
    public Response getTraces() {
        JsonObject result = new JsonObject();
        eventStore.getByTrace().forEach((id, evts) -> result.put(id, toArray(evts)));
        return json(result);
    }

    @GET
    @Path("/traces/{traceId}")
    public Response getTrace(@PathParam("traceId") String traceId) {
        return json(toArray(eventStore.getByTrace(traceId)));
    }

    @DELETE
    @Path("/traces/{traceId}")
    public Response deleteTrace(@PathParam("traceId") String traceId) {
        boolean removed = eventStore.deleteTrace(traceId);
        return removed
                ? Response.noContent().build()
                : Response.status(Response.Status.NOT_FOUND)
                          .entity("{\"error\":\"trace not found\"}")
                          .type(MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/brands")
    public Response getBrands() {
        JsonArray arr = new JsonArray();
        eventStore.getKnownBrands().forEach(arr::add);
        return json(arr);
    }

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
