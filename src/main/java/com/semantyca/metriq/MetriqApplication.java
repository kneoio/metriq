package com.semantyca.metriq;

import com.semantyca.metriq.rest.AivoxProxyRouteResource;
import com.semantyca.metriq.rest.JesoosProxyRouteResource;
import com.semantyca.metriq.rest.MetricEventRouteResource;
import io.vertx.ext.web.Router;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class MetriqApplication {

    @Inject
    MetricEventRouteResource metricEventRouteResource;

    @Inject
    JesoosProxyRouteResource jesoosProxyRouteResource;

    @Inject
    AivoxProxyRouteResource aivoxProxyRouteResource;

    void setupRoutes(@Observes Router router) {
        metricEventRouteResource.setupRoutes(router);
        jesoosProxyRouteResource.setupRoutes(router);
        aivoxProxyRouteResource.setupRoutes(router);
    }
}
