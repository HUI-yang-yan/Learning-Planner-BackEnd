package com.learningplanner.aigateway.mq;

import com.learningplanner.aigateway.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AIProducer {

    private final RabbitTemplate rabbitTemplate;

    public AIProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendGoalAnalysis(Long goalId, String goalName, String goalDesc) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.AI_EXCHANGE, "goal.analysis",
                Map.of("goalId", goalId, "goalName", goalName, "goalDesc", goalDesc));
    }

    public void sendRoadmapGenerate(Long goalId) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.AI_EXCHANGE, "roadmap.generate",
                Map.of("goalId", goalId));
    }

    public void sendTaskGenerate(Long phaseId, String phaseName) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.AI_EXCHANGE, "task.generate",
                Map.of("phaseId", phaseId, "phaseName", phaseName));
    }

    public void sendMasteryEvaluate(Long goalId, double completionRate,
                                     double learningHours, double testScore) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.AI_EXCHANGE, "mastery.evaluate",
                Map.of("goalId", goalId, "completionRate", completionRate,
                        "learningHours", learningHours, "testScore", testScore));
    }
}
