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

@Path("/jesoos")
@ApplicationScoped
public class JesoosProxyResource {

    private static final Logger LOG = Logger.getLogger(JesoosProxyResource.class);

    @ConfigProperty(name = "metriq.jesoos.url", defaultValue = "http://localhost:38797")
    String jesoosUrl;

    @Inject
    Vertx vertx;

    WebClient client;

    @PostConstruct
    void init() {
        client = WebClient.create(vertx);
    }

    @POST
    @Path("/{brand}/start")
    @Produces("application/json")
    public Uni<Response> start(@PathParam("brand") String brand) {
        String url = jesoosUrl + "/jesoos/" + brand + "/start";
        LOG.infof("Proxying POST → %s", url);
        return client.postAbs(url)
                .putHeader("Content-Type", "application/json")
                .send()
                .map(r -> Response.status(r.statusCode())
                        .entity(r.bodyAsString())
                        .header("Content-Type", "application/json")
                        .build())
                .onFailure().recoverWithItem(e -> Response.serverError().entity("{\"error\":\"" + e.getMessage() + "\"}").build());
    }

    @POST
    @Path("/{brand}/stop")
    @Produces("application/json")
    public Uni<Response> stop(@PathParam("brand") String brand) {
        String url = jesoosUrl + "/jesoos/" + brand + "/stop";
        LOG.infof("Proxying POST → %s", url);
        return client.postAbs(url)
                .putHeader("Content-Type", "application/json")
                .send()
                .map(r -> Response.status(r.statusCode())
                        .entity(r.bodyAsString())
                        .header("Content-Type", "application/json")
                        .build())
                .onFailure().recoverWithItem(e -> Response.serverError().entity("{\"error\":\"" + e.getMessage() + "\"}").build());
    }

    @POST
    @Path("/{brand}/enable-dj")
    @Produces("application/json")
    public Uni<Response> enableDj(@PathParam("brand") String brand) {
        String url = jesoosUrl + "/jesoos/" + brand + "/enable-dj";
        LOG.infof("Proxying POST → %s", url);
        return client.postAbs(url)
                .putHeader("Content-Type", "application/json")
                .send()
                .map(r -> Response.status(r.statusCode())
                        .entity(r.bodyAsString())
                        .header("Content-Type", "application/json")
                        .build())
                .onFailure().recoverWithItem(e -> Response.serverError().entity("{\"error\":\"" + e.getMessage() + "\"}").build());
    }

    @POST
    @Path("/{brand}/disable-dj")
    @Produces("application/json")
    public Uni<Response> disableDj(@PathParam("brand") String brand) {
        String url = jesoosUrl + "/jesoos/" + brand + "/disable-dj";
        LOG.infof("Proxying POST → %s", url);
        return client.postAbs(url)
                .putHeader("Content-Type", "application/json")
                .send()
                .map(r -> Response.status(r.statusCode())
                        .entity(r.bodyAsString())
                        .header("Content-Type", "application/json")
                        .build())
                .onFailure().recoverWithItem(e -> Response.serverError().entity("{\"error\":\"" + e.getMessage() + "\"}").build());
    }

    @POST
    @Path("/{brand}/dj-status")
    @Produces("application/json")
    public Uni<Response> djStatus(@PathParam("brand") String brand) {
        String url = jesoosUrl + "/jesoos/" + brand + "/dj-status";
        LOG.infof("Proxying POST → %s", url);
        return client.postAbs(url)
                .putHeader("Content-Type", "application/json")
                .send()
                .map(r -> Response.status(r.statusCode())
                        .entity(r.bodyAsString())
                        .header("Content-Type", "application/json")
                        .build())
                .onFailure().recoverWithItem(e -> Response.serverError().entity("{\"error\":\"" + e.getMessage() + "\"}").build());
    }

    @GET
    @Path("/agendas")
    @Produces("application/json")
    public Uni<Response> agendas() {
        String url = jesoosUrl + "/jesoos/agendas";
        LOG.infof("Proxying GET → %s", url);
        return client.getAbs(url)
                .send()
                .map(r -> Response.status(r.statusCode())
                        .entity(r.bodyAsString())
                        .header("Content-Type", "application/json")
                        .build())
                .onFailure().recoverWithItem(e -> Response.serverError().entity("{\"error\":\"" + e.getMessage() + "\"}").build());
    }

    @POST
    @Path("/stop-all")
    @Produces("application/json")
    public Uni<Response> stopAll() {
        String url = jesoosUrl + "/jesoos/stop-all";
        LOG.infof("Proxying POST → %s", url);
        return client.postAbs(url)
                .putHeader("Content-Type", "application/json")
                .send()
                .map(r -> Response.status(r.statusCode())
                        .entity(r.bodyAsString())
                        .header("Content-Type", "application/json")
                        .build())
                .onFailure().recoverWithItem(e -> Response.serverError().entity("{\"error\":\"" + e.getMessage() + "\"}").build());
    }
}
