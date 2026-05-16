package com.learningplanner.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class GatewayRoutesConfig {

    private static final Logger log = LoggerFactory.getLogger(GatewayRoutesConfig.class);

    @Bean
    public RouteLocator customRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                // User Service
                .route("user-service", r -> r
                        .path("/api/user/**")
                        .filters(f -> f.filter((exchange, chain) -> {
                            log.info("[Gateway] Routing to user-service: {} {}",
                                    exchange.getRequest().getMethod(),
                                    exchange.getRequest().getURI().getPath());
                            return chain.filter(exchange);
                        }))
                        .uri("lb://user-service"))
                // AI Planner Chat Service
                .route("ai-planner-chat", r -> r
                        .path("/api/planner/ai/chat")
                        .uri("lb://ai-planner-service"))
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
