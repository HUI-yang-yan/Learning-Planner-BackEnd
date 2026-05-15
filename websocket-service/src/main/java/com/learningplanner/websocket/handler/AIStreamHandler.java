package com.learningplanner.websocket.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class AIStreamHandler implements WebSocketHandler {

    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, String> redisTemplate;

    public AIStreamHandler(ObjectMapper objectMapper, RedisTemplate<String, String> redisTemplate) {
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
        log.info("[WebSocket] Connection established: {}", session.getId());
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {}

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        sessions.remove(session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
        log.info("[WebSocket] Connection closed: {}", session.getId());
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    public void sendAIMessage(String sessionId, String type, String content) {
        WebSocketSession session = sessions.get(sessionId);
        if (session != null && session.isOpen()) {
            try {
                String json = objectMapper.writeValueAsString(
                        Map.of("type", type, "content", content));
                session.sendMessage(new TextMessage(json));
            } catch (IOException e) {
                sessions.remove(sessionId);
            }
        }
    }

    public void broadcastAIMessage(String type, String content) {
        try {
            String json = objectMapper.writeValueAsString(
                    Map.of("type", type, "content", content));
            TextMessage msg = new TextMessage(json);
            for (WebSocketSession session : sessions.values()) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(msg);
                    } catch (IOException ignored) {
                    }
                }
            }
        } catch (IOException ignored) {
        }
    }

    public void publishAIStreamEvent(Long goalId, String type, String content) {
        try {
            String json = objectMapper.writeValueAsString(
                    Map.of("goalId", goalId, "type", type, "content", content));
            redisTemplate.convertAndSend("ai-stream-channel", json);
        } catch (IOException ignored) {
        }
    }

    @SuppressWarnings("unchecked")
    public void handleRedisMessage(String message) {
        try {
            Map<String, Object> msg = objectMapper.readValue(message, Map.class);
            // Forward to all connected WebSocket clients
            broadcastAIMessage(
                    (String) msg.getOrDefault("type", "PROGRESS"),
                    (String) msg.getOrDefault("content", ""));
        } catch (IOException e) {
            log.error("[WebSocket] Failed to parse Redis message: {}", e.getMessage());
        }
    }
}
