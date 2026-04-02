package com.semantyca.metriq.rest;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

@ApplicationScoped
public class UserDashboardResource {
    private static final Logger LOGGER = Logger.getLogger(UserDashboardResource.class);


    public void setupRoutes(Router router) {
        String path = "/metriq";
        router.route(HttpMethod.GET, path + "/playlist/:brand").handler(this::getPlaylist);

    }

    private void getPlaylist(RoutingContext rc) {
        try {

            JsonObject result = new JsonObject();

            rc.response()
                    .setStatusCode(200)
                    .putHeader("Content-Type", "application/json")
                    .end(result.encode());
        } catch (Exception e) {
            LOGGER.error("Failed to get cleanup stats", e);
            rc.response()
                    .setStatusCode(500)
                    .putHeader("Content-Type", "application/json")
                    .end(new JsonObject().put("error", e.getMessage()).encode());
        }
    }

}
