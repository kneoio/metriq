package com.semantyca.metriq.rest;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@Path("/")
@ApplicationScoped
public class AivoxProxyResource {

    private static final Logger LOG = Logger.getLogger(AivoxProxyResource.class);

    @ConfigProperty(name = "metriq.aivox.url", defaultValue = "http://localhost:38798")
    String aivoxUrl;

    @Inject
    Vertx vertx;

    WebClient client;

    @PostConstruct
    void init() {
        client = WebClient.create(vertx);
    }

    @POST
    @Path("/aivox/{brand}/start")
    public Uni<Response> startStream(@PathParam("brand") String brand) {
        String url = aivoxUrl + "/aivox/" + brand + "/start";
        LOG.infof("Proxying POST → %s", url);
        return client.postAbs(url).putHeader("X-Client-ID", "mixpla-web").send()
                .map(r -> Response.status(r.statusCode()).entity(r.bodyAsString()).build())
                .onFailure().recoverWithItem(e -> Response.serverError().entity(e.getMessage()).build());
    }

    @DELETE
    @Path("/aivox/{brand}/stop")
    public Uni<Response> stopStream(@PathParam("brand") String brand) {
        String url = aivoxUrl + "/aivox/" + brand + "/stop";
        LOG.infof("Proxying DELETE → %s", url);
        return client.deleteAbs(url).putHeader("X-Client-ID", "mixpla-web").send()
                .map(r -> Response.status(r.statusCode()).entity(r.bodyAsString()).build())
                .onFailure().recoverWithItem(e -> Response.serverError().entity(e.getMessage()).build());
    }

    @GET
    @Path("/stream/{brand}/{path:.+}")
    public Uni<Response> proxyStream(@PathParam("brand") String brand, @PathParam("path") String path) {
        String url = aivoxUrl + "/stream/" + brand + "/" + path;
        LOG.debugf("Proxying stream GET → %s", url);
        return client.getAbs(url).send()
                .map(r -> {
                    String ct = r.getHeader("Content-Type");
                    Response.ResponseBuilder rb = Response.status(r.statusCode())
                            .entity(r.body().getBytes())
                            .header("Content-Type", ct != null ? ct : "application/octet-stream");
                    String cc = r.getHeader("Cache-Control");
                    if (cc != null) rb.header("Cache-Control", cc);
                    return rb.build();
                })
                .onFailure().recoverWithItem(e -> {
                    LOG.errorf("Stream proxy failed: %s", e.getMessage());
                    return Response.serverError().build();
                });
    }
}
