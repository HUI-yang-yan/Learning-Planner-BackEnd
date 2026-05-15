package com.learningplanner.planner.mq;

import com.learningplanner.common.dto.AIResultRequest;
import com.learningplanner.common.entity.LearningGoal;
import com.learningplanner.common.entity.LearningPhase;
import com.learningplanner.planner.config.RabbitMQConfig;
import com.learningplanner.planner.service.LearningGoalService;
import com.learningplanner.planner.service.LearningPhaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AIResultConsumer {

    private final LearningGoalService goalService;
    private final LearningPhaseService phaseService;

    public AIResultConsumer(LearningGoalService goalService,
                            LearningPhaseService phaseService) {
        this.goalService = goalService;
        this.phaseService = phaseService;
    }

    @RabbitListener(queues = RabbitMQConfig.AI_RESULT_QUEUE)
    public void handleAIResult(AIResultRequest result) {
        log.info("[AI Result] Received AI result: goalId={}, phases={}",
                result.getGoalId(),
                result.getPhases() != null ? result.getPhases().size() : 0);
        try {
            LearningGoal goal = goalService.getById(result.getGoalId());
            if (goal == null) {
                log.error("[AI Result] Goal not found: {}", result.getGoalId());
                return;
            }
            if (result.getPhases() != null) {
                for (var dto : result.getPhases()) {
                    LearningPhase phase = new LearningPhase();
                    phase.setGoalId(result.getGoalId());
                    phase.setPhaseName(dto.getPhaseName());
                    phase.setPhaseOrder(dto.getPhaseOrder());
                    phase.setPhaseDesc(dto.getPhaseDesc());
                    phase.setEstimatedDays(dto.getEstimatedDays());
                    phase.setStatus("PENDING");
                    phaseService.save(phase);

                    if (dto.getTasks() != null) {
                        for (var t : dto.getTasks()) {
                            // TASK: via Feign to task-service (next step)
                            // For now, phases are saved; tasks via task-service Feign
                        }
                    }
                }
            }
            goal.setStatus("ACTIVE");
            goalService.updateById(goal);
            log.info("[AI Result] Saved: goalId={}", result.getGoalId());
        } catch (Exception e) {
            log.error("[AI Result] Save failed: goalId={}", result.getGoalId(), e);
            throw e;
        }
    }
}
