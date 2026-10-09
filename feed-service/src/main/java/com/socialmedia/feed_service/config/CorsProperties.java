package com.socialmedia.feed_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties("cors")
public record CorsProperties(List<String> allowedOrigins) {
}
