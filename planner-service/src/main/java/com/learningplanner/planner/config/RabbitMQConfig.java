package com.learningplanner.planner.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String AI_EXCHANGE = "ai.exchange";
    public static final String GOAL_ANALYSIS_QUEUE = "goal.analysis.queue";

    @Bean
    public Queue goalAnalysisQueue() {
        return QueueBuilder.durable(GOAL_ANALYSIS_QUEUE).build();
    }

    @Bean
    public DirectExchange aiExchange() {
        return new DirectExchange(AI_EXCHANGE);
    }

    @Bean
    public Binding goalAnalysisBinding() {
        return BindingBuilder.bind(goalAnalysisQueue()).to(aiExchange()).with("goal.analysis");
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
