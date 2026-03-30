package com.semantyca.metriq.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

import java.util.List;
import java.util.Optional;

@ConfigMapping(prefix = "metriq")
public interface MetriqConfig {

    @WithName("jesoos.url")
    @WithDefault("http://localhost:38797")
    String getJesoosUrl();

    @WithName("aivox.url")
    @WithDefault("http://localhost:38798")
    String getAivoxUrl();

    Cleanup cleanup();

    interface Cleanup {
        Optional<List<String>> folders();

        @WithDefault("24")
        long maxAgeHours();
    }

}