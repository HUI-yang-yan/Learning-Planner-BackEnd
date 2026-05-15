package com.learningplanner.planner.controller;

import com.learningplanner.common.dto.GoalRequest;
import com.learningplanner.common.dto.Result;
import com.learningplanner.common.entity.LearningGoal;
import com.learningplanner.planner.service.LearningGoalService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/planner/goals")
public class LearningGoalController {

    private final LearningGoalService goalService;

    public LearningGoalController(LearningGoalService goalService) {
        this.goalService = goalService;
    }

    @PostMapping
    public Result<LearningGoal> create(@RequestBody @Valid GoalRequest request) {
        return Result.ok(goalService.create(request));
    }

    @GetMapping("/{id}")
    public Result<LearningGoal> getById(@PathVariable Long id) {
        return Result.ok(goalService.getById(id));
    }

    @GetMapping
    public Result<?> page(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int size) {
        return Result.ok(goalService.pageByUser(page, size));
    }

    @PutMapping("/{id}")
    public Result<LearningGoal> update(@PathVariable Long id, @RequestBody GoalRequest request) {
        LearningGoal goal = goalService.getById(id);
        if (goal != null) {
            goal.setGoalName(request.getGoalName());
            goal.setGoalDesc(request.getGoalDesc());
            goal.setEstimatedDuration(request.getEstimatedDuration());
            goalService.updateById(goal);
        }
        return Result.ok(goal);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        goalService.removeById(id);
        return Result.ok();
    }

    @PutMapping("/{id}/status")
    public Result<LearningGoal> updateStatus(@PathVariable Long id, @RequestParam String status) {
        return Result.ok(goalService.updateStatus(id, status));
    }
}
