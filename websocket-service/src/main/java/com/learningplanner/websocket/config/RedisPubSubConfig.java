package com.learningplanner.websocket.config;

import com.learningplanner.websocket.handler.AIStreamHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
public class RedisPubSubConfig {

    @Bean
    public RedisMessageListenerContainer redisContainer(RedisConnectionFactory factory,
                                                         MessageListenerAdapter adapter) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);
        container.addMessageListener(adapter, new PatternTopic("ai-stream-channel"));
        return container;
    }

    @Bean
    public MessageListenerAdapter listenerAdapter(AIStreamHandler handler) {
        return new MessageListenerAdapter(handler, "handleRedisMessage");
    }
}
