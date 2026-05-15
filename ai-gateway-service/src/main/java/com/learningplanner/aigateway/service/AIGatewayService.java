package com.learningplanner.aigateway.service;

import com.learningplanner.aigateway.mq.AIProducer;
import com.learningplanner.common.dto.GoalRequest;
import org.springframework.stereotype.Service;

@Service
public class AIGatewayService {

    private final AIProducer aiProducer;

    public AIGatewayService(AIProducer aiProducer) {
        this.aiProducer = aiProducer;
    }

    public void triggerGoalAnalysis(Long goalId, String goalName, String goalDesc) {
        aiProducer.sendGoalAnalysis(goalId, goalName, goalDesc);
    }

    public void triggerRoadmapGenerate(Long goalId) {
        aiProducer.sendRoadmapGenerate(goalId);
    }

    public void triggerTaskGenerate(Long phaseId, String phaseName) {
        aiProducer.sendTaskGenerate(phaseId, phaseName);
    }

    public void triggerMasteryEvaluate(Long goalId, double completionRate,
                                        double learningHours, double testScore) {
        aiProducer.sendMasteryEvaluate(goalId, completionRate, learningHours, testScore);
    }
}
