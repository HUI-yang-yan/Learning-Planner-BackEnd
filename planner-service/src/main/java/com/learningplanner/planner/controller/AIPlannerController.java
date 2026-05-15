package com.learningplanner.planner.controller;

import com.learningplanner.planner.mq.AIProducer;
import com.learningplanner.common.dto.Result;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/planner/ai")
public class AIPlannerController {

    private final AIProducer aiProducer;

    public AIPlannerController(AIProducer aiProducer) {
        this.aiProducer = aiProducer;
    }

    @PostMapping("/mastery/evaluate")
    public Result<Void> evaluateMastery(@RequestParam Long goalId,
                                         @RequestParam double completionRate,
                                         @RequestParam double learningHours,
                                         @RequestParam double testScore) {
        aiProducer.sendMasteryEvaluate(goalId, completionRate, learningHours, testScore);
        return Result.ok();
    }
}
