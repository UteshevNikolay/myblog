package com.my.blog.project.myblogonboot.myblog.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
public class WebConfig implements WebMvcConfigurer {

    @Value("${cors.allowed-origins:http://localhost}")
    private String[] allowedOrigins;

    @Value("${cors.allowed-methods:*}")
    private String[] allowedMethods;

    @Value("${cors.max-age:3600}")
    private long maxAge;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods(allowedMethods)
                .allowedHeaders("*")
                .exposedHeaders("Content-Type", "Content-Length", "Content-Disposition")
                .allowCredentials(true)
                .maxAge(maxAge);
    }
}
