package com.learningplanner.planner.mq;

import com.learningplanner.planner.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class GoalAnalysisProducer {

    private final RabbitTemplate rabbitTemplate;

    public GoalAnalysisProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendGoalAnalysis(Long goalId, Long userId, String goalName, String goalDesc) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.AI_EXCHANGE, "goal.analysis",
                Map.of("goalId", goalId, "userId", userId,
                       "goalName", goalName, "goalDesc", goalDesc));
    }
}
