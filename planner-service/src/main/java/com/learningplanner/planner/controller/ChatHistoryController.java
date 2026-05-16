package com.learningplanner.planner.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.learningplanner.common.dto.Result;
import com.learningplanner.common.entity.ChatMessage;
import com.learningplanner.planner.repository.ChatMessageMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Tag(name = "聊天历史", description = "分页查询对话历史记录")
@RestController
@RequestMapping("/api/planner/chat")
public class ChatHistoryController {

    private final ChatMessageMapper chatMessageMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    public ChatHistoryController(ChatMessageMapper chatMessageMapper,
                                  RedisTemplate<String, Object> redisTemplate) {
        this.chatMessageMapper = chatMessageMapper;
        this.redisTemplate = redisTemplate;
    }

    @Operation(summary = "分页查询对话历史", description = "按 conversationId 分页拉取消息，每页 20 条")
    @GetMapping("/history")
    public Result<Map<String, Object>> getHistory(
            @Parameter(description = "会话 ID") @RequestParam String conversationId,
            @Parameter(description = "偏移量") @RequestParam(defaultValue = "0") int offset) {
        Long userId = StpUtil.getLoginIdAsLong();
        String cacheKey = "chat:history:" + userId + ":" + conversationId + ":" + (offset / 20);

        // 尝试从 Redis 缓存读取
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof List) {
            @SuppressWarnings("unchecked")
            List<ChatMessage> list = (List<ChatMessage>) cached;
            return Result.ok(Map.of("items", list, "hasMore", list.size() == 20, "nextOffset", offset + list.size()));
        }

        // 缓存未命中，查 MySQL
        List<ChatMessage> messages = chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getConversationId, conversationId)
                        .orderByAsc(ChatMessage::getId)
                        .last("LIMIT 20 OFFSET " + offset));

        // 写入 Redis 缓存（10 分钟过期）
        redisTemplate.opsForValue().set(cacheKey, messages, 10, TimeUnit.MINUTES);

        return Result.ok(Map.of(
                "items", messages,
                "hasMore", messages.size() == 20,
                "nextOffset", offset + messages.size()));
    }
}
