package com.learningplanner.aigateway.controller;

import com.learningplanner.aigateway.service.AIGatewayService;
import com.learningplanner.common.dto.AIResultRequest;
import com.learningplanner.common.dto.Result;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AIGatewayController {

    private final AIGatewayService aiGatewayService;

    public AIGatewayController(AIGatewayService aiGatewayService) {
        this.aiGatewayService = aiGatewayService;
    }

    @PostMapping("/goal/analyze")
    public Result<Void> analyzeGoal(@RequestParam Long goalId,
                                     @RequestParam String goalName,
                                     @RequestParam(required = false) String goalDesc) {
        aiGatewayService.triggerGoalAnalysis(goalId, goalName,
                goalDesc != null ? goalDesc : "");
        return Result.ok();
    }

    @PostMapping("/roadmap/generate")
    public Result<Void> generateRoadmap(@RequestParam Long goalId) {
        aiGatewayService.triggerRoadmapGenerate(goalId);
        return Result.ok();
    }

    @PostMapping("/task/generate")
    public Result<Void> generateTasks(@RequestParam Long phaseId,
                                       @RequestParam String phaseName) {
        aiGatewayService.triggerTaskGenerate(phaseId, phaseName);
        return Result.ok();
    }

    @PostMapping("/mastery/evaluate")
    public Result<Void> evaluateMastery(@RequestParam Long goalId,
                                         @RequestParam double completionRate,
                                         @RequestParam double learningHours,
                                         @RequestParam double testScore) {
        aiGatewayService.triggerMasteryEvaluate(goalId, completionRate, learningHours, testScore);
        return Result.ok();
    }

    @PostMapping("/result")
    public Result<Void> receiveAIResult(@RequestBody AIResultRequest result) {
        return Result.ok();
    }
}
