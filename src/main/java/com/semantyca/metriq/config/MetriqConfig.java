package com.semantyca.metriq.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "metriq")
public interface MetriqConfig {

    @WithName("jesoos.url")
    @WithDefault("http://localhost:38797")
    String getJesoosUrl();

    @WithName("aivox.url")
    @WithDefault("http://localhost:38798")
    String getAivoxUrl();

    Path path();

    String getPathUploads();

    interface Path {
        @WithDefault("uploads")
        String uploads();

        @WithDefault("temp")
        String temp();
    }

}