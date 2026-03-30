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
public class AivoxProxyRouteResource {
    private static final Logger LOGGER = Logger.getLogger(AivoxProxyRouteResource.class);

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
        router.route(HttpMethod.POST, "/aivox/:brand/start").handler(this::startStream);
        router.route(HttpMethod.DELETE, "/aivox/:brand/stop").handler(this::stopStream);
    }

    private void startStream(RoutingContext rc) {
        String brand = rc.pathParam("brand");
        String baseUrl = config.getAivoxUrl();
        String url = baseUrl + "/aivox/command/" + brand + "/start";
        LOGGER.infof("Proxying POST → %s", url);
        
        client.postAbs(url)
                .putHeader("X-Client-ID", "mixpla-web")
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

    private void stopStream(RoutingContext rc) {
        String brand = rc.pathParam("brand");
        String url = config.getAivoxUrl() + "/aivox/command/" + brand + "/stop";
        LOGGER.infof("Proxying DELETE → %s", url);
        
        client.deleteAbs(url)
                .putHeader("X-Client-ID", "mixpla-web")
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
