package com.learningplanner.planner.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String GOAL_ANALYSIS_QUEUE = "goal.analysis.queue";
    public static final String ROADMAP_GENERATE_QUEUE = "roadmap.generate.queue";
    public static final String TASK_GENERATE_QUEUE = "task.generate.queue";
    public static final String MASTERY_EVALUATE_QUEUE = "mastery.evaluate.queue";
    public static final String AI_RESULT_QUEUE = "ai.result.queue";
    public static final String AI_EXCHANGE = "ai.exchange";

    @Bean
    public Queue goalAnalysisQueue() { return QueueBuilder.durable(GOAL_ANALYSIS_QUEUE).build(); }
    @Bean
    public Queue roadmapGenerateQueue() { return QueueBuilder.durable(ROADMAP_GENERATE_QUEUE).build(); }
    @Bean
    public Queue taskGenerateQueue() { return QueueBuilder.durable(TASK_GENERATE_QUEUE).build(); }
    @Bean
    public Queue masteryEvaluateQueue() { return QueueBuilder.durable(MASTERY_EVALUATE_QUEUE).build(); }
    @Bean
    public Queue aiResultQueue() { return QueueBuilder.durable(AI_RESULT_QUEUE).build(); }

    @Bean
    public DirectExchange aiExchange() { return new DirectExchange(AI_EXCHANGE); }

    @Bean
    public Binding goalAnalysisBinding() { return BindingBuilder.bind(goalAnalysisQueue()).to(aiExchange()).with("goal.analysis"); }
    @Bean
    public Binding roadmapGenerateBinding() { return BindingBuilder.bind(roadmapGenerateQueue()).to(aiExchange()).with("roadmap.generate"); }
    @Bean
    public Binding taskGenerateBinding() { return BindingBuilder.bind(taskGenerateQueue()).to(aiExchange()).with("task.generate"); }
    @Bean
    public Binding masteryEvaluateBinding() { return BindingBuilder.bind(masteryEvaluateQueue()).to(aiExchange()).with("mastery.evaluate"); }
    @Bean
    public Binding aiResultBinding() { return BindingBuilder.bind(aiResultQueue()).to(aiExchange()).with("ai.result"); }

    @Bean
    public MessageConverter messageConverter() { return new Jackson2JsonMessageConverter(); }
}
