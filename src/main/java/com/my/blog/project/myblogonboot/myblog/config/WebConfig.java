package com.my.blog.project.myblogonboot.myblog.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedMethods("*")
                .allowedOrigins("http://localhost/")
                .allowedHeaders("Content-Type", "Authorization", "X-Custom-Header")
                .exposedHeaders("X-Custom-Header")
                .allowCredentials(true)
                .maxAge(3600); // Cache preflight response for 1 hour (3600 seconds)
    }
}
