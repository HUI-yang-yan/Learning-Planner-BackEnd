package com.learningplanner.aigateway.mq;

import com.learningplanner.aigateway.config.RabbitMQConfig;
import com.learningplanner.aigateway.feign.PlannerFeignClient;
import com.learningplanner.common.dto.AIResultRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AIResultConsumer {

    private final PlannerFeignClient plannerFeignClient;

    public AIResultConsumer(PlannerFeignClient plannerFeignClient) {
        this.plannerFeignClient = plannerFeignClient;
    }

    @RabbitListener(queues = RabbitMQConfig.AI_RESULT_QUEUE)
    public void handleAIResult(AIResultRequest result) {
        log.info("[AI Result] Received AI result: goalId={}, phases={}",
                result.getGoalId(),
                result.getPhases() != null ? result.getPhases().size() : 0);
        try {
            plannerFeignClient.saveAIResult(result.getGoalId(), result);
            log.info("[AI Result] Result saved to planner-service: goalId={}", result.getGoalId());
        } catch (Exception e) {
            log.error("[AI Result] Save failed: goalId={}", result.getGoalId(), e);
            throw e;
        }
    }
}
