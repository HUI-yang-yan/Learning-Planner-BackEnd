package com.learningplanner.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutesConfig {

    @Bean
    public RouteLocator customRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                // User Service
                .route("user-service", r -> r
                        .path("/api/user/**")
                        .uri("lb://user-service"))
                // Planner Service
                .route("planner-service", r -> r
                        .path("/api/planner/**")
                        .uri("lb://planner-service"))
                // Task Service
                .route("task-service", r -> r
                        .path("/api/task/**")
                        .uri("lb://task-service"))
                // Reminder Service
                .route("reminder-service", r -> r
                        .path("/api/reminders/**")
                        .uri("lb://reminder-service"))
                // WebSocket Service
                .route("websocket-service", r -> r
                        .path("/ws/**")
                        .uri("lb://websocket-service"))
                .build();
    }
}
