package com.example.education_library.config;

import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
public class SwaggerConfig {

    @PostConstruct
    public void init() {
        // Globally ignore org.springframework.core.io.Resource to prevent Swagger scan crashes
        SpringDocUtils.getConfig().replaceWithClass(org.springframework.core.io.Resource.class, Void.class);
    }
}
