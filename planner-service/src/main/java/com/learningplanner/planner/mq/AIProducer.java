package com.learningplanner.planner.mq;

import com.learningplanner.planner.config.RabbitMQConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class AIProducer {

    private final RabbitTemplate rabbitTemplate;

    public AIProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendGoalAnalysis(Long goalId, Long userId, String goalName, String goalDesc) {
        log.info("[AI Producer] Sending goal analysis: goalId={}, goalName={}", goalId, goalName);
        rabbitTemplate.convertAndSend(RabbitMQConfig.AI_EXCHANGE, "goal.analysis",
                Map.of("goalId", goalId, "userId", userId,
                       "goalName", goalName, "goalDesc", goalDesc));
        log.info("[AI Producer] Goal analysis sent to exchange={}, routingKey=goal.analysis",
                RabbitMQConfig.AI_EXCHANGE);
    }

    public void sendRoadmapGenerate(Long goalId) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.AI_EXCHANGE, "roadmap.generate",
                Map.of("goalId", goalId));
    }

    public void sendTaskGenerate(Long phaseId, String phaseName) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.AI_EXCHANGE, "task.generate",
                Map.of("phaseId", phaseId, "phaseName", phaseName));
    }

    public void sendMasteryEvaluate(Long goalId, Long userId, String goalName,
                                     double completionRate, double learningHours,
                                     double testScore) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.AI_EXCHANGE, "mastery.evaluate",
                Map.of("goalId", goalId, "userId", userId, "goalName", goalName,
                        "completionRate", completionRate,
                        "learningHours", learningHours, "testScore", testScore));
    }
}
