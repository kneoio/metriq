package com.semantyca.metriq.rest;

import com.semantyca.metriq.config.MetriqConfig;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class JesoosProxyRouteResource {
    private static final Logger LOGGER = Logger.getLogger(JesoosProxyRouteResource.class);

    @Inject
    Vertx vertx;

    @Inject
    MetriqConfig config;

    WebClient client;

    @PostConstruct
    void init() {
        client = WebClient.create(vertx);
    }

    public void setupRoutes(Router router) {
        String path = "/jesoos";
        router.route(HttpMethod.GET, path + "/info/:brand/live").handler(this::getLiveStatus);
        router.route(HttpMethod.GET, path + "/info/:brand/dj-status").handler(this::getDjStatus);
        router.route(HttpMethod.GET, path + "/info/:brand/agendas").handler(this::getAgendas);
        
        router.route(HttpMethod.POST, path + "/command/:brand/start").handler(this::handleStart);
        router.route(HttpMethod.POST, path + "/command/:brand/stop").handler(this::handleStop);
        router.route(HttpMethod.POST, path + "/command/:brand/enable-dj").handler(this::handleEnableDj);
        router.route(HttpMethod.POST, path + "/command/:brand/disable-dj").handler(this::handleDisableDj);
        router.route(HttpMethod.POST, path + "/command/:brand/emit-timeline-entry/:sceneId/:seqNumber").handler(this::handleEmitTimelineEntry);
    }

    private void handleStart(RoutingContext rc) {
        String brand = rc.pathParam("brand");
        String url = config.getJesoosUrl() + "/jesoos/command/" + brand + "/start";
        LOGGER.infof("Proxying POST → %s", url);
        
        client.postAbs(url)
                .putHeader("Content-Type", "application/json")
                .send()
                .subscribe().with(
                    r -> rc.response()
                            .setStatusCode(r.statusCode())
                            .putHeader("Content-Type", "application/json")
                            .end(r.bodyAsString()),
                    e -> rc.response()
                            .setStatusCode(500)
                            .putHeader("Content-Type", "application/json")
                            .end(new JsonObject().put("error", e.getMessage()).encode())
                );
    }

    private void handleStop(RoutingContext rc) {
        String brand = rc.pathParam("brand");
        String url = config.getJesoosUrl() + "/jesoos/command/" + brand + "/stop";
        LOGGER.infof("Proxying POST → %s", url);
        
        client.postAbs(url)
                .putHeader("Content-Type", "application/json")
                .send()
                .subscribe().with(
                    r -> rc.response()
                            .setStatusCode(r.statusCode())
                            .putHeader("Content-Type", "application/json")
                            .end(r.bodyAsString()),
                    e -> rc.response()
                            .setStatusCode(500)
                            .putHeader("Content-Type", "application/json")
                            .end(new JsonObject().put("error", e.getMessage()).encode())
                );
    }

    private void handleEnableDj(RoutingContext rc) {
        String brand = rc.pathParam("brand");
        String url = config.getJesoosUrl() + "/jesoos/command/" + brand + "/enable-dj";
        LOGGER.infof("Proxying POST → %s", url);
        
        client.postAbs(url)
                .putHeader("Content-Type", "application/json")
                .send()
                .subscribe().with(
                    r -> rc.response()
                            .setStatusCode(r.statusCode())
                            .putHeader("Content-Type", "application/json")
                            .end(r.bodyAsString()),
                    e -> rc.response()
                            .setStatusCode(500)
                            .putHeader("Content-Type", "application/json")
                            .end(new JsonObject().put("error", e.getMessage()).encode())
                );
    }

    private void handleDisableDj(RoutingContext rc) {
        String brand = rc.pathParam("brand");
        String url = config.getJesoosUrl() + "/jesoos/command/" + brand + "/disable-dj";
        LOGGER.infof("Proxying POST → %s", url);
        
        client.postAbs(url)
                .putHeader("Content-Type", "application/json")
                .send()
                .subscribe().with(
                    r -> rc.response()
                            .setStatusCode(r.statusCode())
                            .putHeader("Content-Type", "application/json")
                            .end(r.bodyAsString()),
                    e -> rc.response()
                            .setStatusCode(500)
                            .putHeader("Content-Type", "application/json")
                            .end(new JsonObject().put("error", e.getMessage()).encode())
                );
    }

    private void handleEmitTimelineEntry(RoutingContext rc) {
        String brand     = rc.pathParam("brand");
        String sceneId   = rc.pathParam("sceneId");
        String seqNumber = rc.pathParam("seqNumber");
        String url = config.getJesoosUrl() + "/jesoos/command/" + brand + "/emit-timeline-entry/" + sceneId + "/" + seqNumber;
        LOGGER.infof("Proxying POST → %s", url);

        client.postAbs(url)
                .putHeader("Content-Type", "application/json")
                .send()
                .subscribe().with(
                    r -> rc.response()
                            .setStatusCode(r.statusCode())
                            .putHeader("Content-Type", "application/json")
                            .end(r.bodyAsString()),
                    e -> rc.response()
                            .setStatusCode(500)
                            .putHeader("Content-Type", "application/json")
                            .end(new JsonObject().put("error", e.getMessage()).encode())
                );
    }

    private void getLiveStatus(RoutingContext rc) {
        String brand = rc.pathParam("brand");
        String url = config.getJesoosUrl() + "/jesoos/info/" + brand + "/live";
        LOGGER.infof("Proxying GET → %s", url);

        client.getAbs(url)
                .send()
                .subscribe().with(
                    r -> rc.response()
                            .setStatusCode(r.statusCode())
                            .putHeader("Content-Type", "application/json")
                            .end(r.bodyAsString()),
                    e -> rc.response()
                            .setStatusCode(500)
                            .putHeader("Content-Type", "application/json")
                            .end(new JsonObject().put("error", e.getMessage()).encode())
                );
    }

    private void getDjStatus(RoutingContext rc) {
        String brand = rc.pathParam("brand");
        String url = config.getJesoosUrl() + "/jesoos/info/" + brand + "/dj-status";
        LOGGER.infof("Proxying GET → %s", url);
        
        client.getAbs(url)
                .send()
                .subscribe().with(
                    r -> rc.response()
                            .setStatusCode(r.statusCode())
                            .putHeader("Content-Type", "application/json")
                            .end(r.bodyAsString()),
                    e -> rc.response()
                            .setStatusCode(500)
                            .putHeader("Content-Type", "application/json")
                            .end(new JsonObject().put("error", e.getMessage()).encode())
                );
    }

    private void getAgendas(RoutingContext rc) {
        String brand = rc.pathParam("brand");
        String url = config.getJesoosUrl() + "/jesoos/info/" + brand + "/agendas";
        LOGGER.infof("Proxying GET → %s", url);
        
        client.getAbs(url)
                .send()
                .subscribe().with(
                    r -> rc.response()
                            .setStatusCode(r.statusCode())
                            .putHeader("Content-Type", "application/json")
                            .end(r.bodyAsString()),
                    e -> rc.response()
                            .setStatusCode(500)
                            .putHeader("Content-Type", "application/json")
                            .end(new JsonObject().put("error", e.getMessage()).encode())
                );
    }
}
