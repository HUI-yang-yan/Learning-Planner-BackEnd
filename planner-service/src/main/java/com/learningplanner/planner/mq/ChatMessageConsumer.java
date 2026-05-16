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
        log.info("[Chat] Saving message: convId={}, role={}",
                msg.get("conversationId"), msg.get("role"));
        try {
            ChatMessage entity = new ChatMessage();
            entity.setUserId(Long.valueOf(msg.get("userId").toString()));
            entity.setConversationId((String) msg.get("conversationId"));
            entity.setRole((String) msg.get("role"));
            entity.setContent((String) msg.get("content"));
            chatMessageMapper.insert(entity);
        } catch (Exception e) {
            log.error("[Chat] Failed to save message", e);
            throw e;
        }
    }
}
