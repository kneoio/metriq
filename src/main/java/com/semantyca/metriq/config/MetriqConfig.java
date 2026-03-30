package com.semantyca.metriq.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

import java.util.List;
import java.util.Optional;

@ConfigMapping(prefix = "aivox")
public interface MetriqConfig {

    @WithDefault("http://localhost:8080")
    String host();

    @WithName("agent.url")
    @WithDefault("http://localhost:38799")
    String getAgentUrl();

    Path path();

    interface Path {
        @WithDefault("uploads")
        String uploads();

        @WithDefault("temp")
        String temp();
    }

}