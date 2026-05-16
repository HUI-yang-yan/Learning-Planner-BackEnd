package com.learningplanner.planner.mq;

import com.learningplanner.common.entity.ChatMessage;
import com.learningplanner.planner.config.RabbitMQConfig;
import com.learningplanner.planner.repository.ChatMessageMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class ChatMessageConsumer {

    private final ChatMessageMapper chatMessageMapper;

    public ChatMessageConsumer(ChatMessageMapper chatMessageMapper) {
        this.chatMessageMapper = chatMessageMapper;
    }

    @SuppressWarnings("unchecked")
    @RabbitListener(queues = RabbitMQConfig.CHAT_MESSAGE_QUEUE)
    public void handleChatMessage(Map<String, Object> msg) {
        Object userIdObj = msg.get("userId");
        String conversationId = (String) msg.get("conversationId");
        String role = (String) msg.get("role");
        String content = (String) msg.get("content");

        if (userIdObj == null || conversationId == null || role == null || content == null) {
            log.warn("[Chat] Skipping malformed message: {}", msg);
            return;
        }

        log.info("[Chat] Saving message: convId={}, role={}", conversationId, role);
        try {
            ChatMessage entity = new ChatMessage();
            entity.setUserId(Long.valueOf(userIdObj.toString()));
            entity.setConversationId(conversationId);
            entity.setRole(role);
            entity.setContent(content);
            chatMessageMapper.insert(entity);
        } catch (Exception e) {
            log.error("[Chat] Failed to save message", e);
            throw e;
        }
    }
}
