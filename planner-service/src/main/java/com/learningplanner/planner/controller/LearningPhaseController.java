package com.learningplanner.planner.controller;

import com.learningplanner.common.dto.PhaseRequest;
import com.learningplanner.common.dto.Result;
import com.learningplanner.common.entity.LearningPhase;
import com.learningplanner.planner.service.LearningPhaseService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/phase")
public class LearningPhaseController {

    private final LearningPhaseService phaseService;

    public LearningPhaseController(LearningPhaseService phaseService) {
        this.phaseService = phaseService;
    }

    @PostMapping
    public Result<LearningPhase> create(@RequestBody @Valid PhaseRequest request) {
        return Result.ok(phaseService.create(request));
    }

    @GetMapping("/{id}")
    public Result<LearningPhase> getById(@PathVariable Long id) {
        return Result.ok(phaseService.getById(id));
    }

    @GetMapping("/page")
    public Result<?> page(@RequestParam Long goalId,
                          @RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int size) {
        return Result.ok(phaseService.pageByGoal(goalId, page, size));
    }

    @PutMapping("/{id}")
    public Result<LearningPhase> update(@PathVariable Long id, @RequestBody PhaseRequest request) {
        LearningPhase phase = phaseService.getById(id);
        if (phase != null) {
            phase.setPhaseName(request.getPhaseName());
            phase.setPhaseOrder(request.getPhaseOrder());
            phaseService.updateById(phase);
        }
        return Result.ok(phase);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        phaseService.removeById(id);
        return Result.ok();
    }

    @PutMapping("/{id}/mastery")
    public Result<LearningPhase> updateMastery(@PathVariable Long id, @RequestParam Integer score) {
        return Result.ok(phaseService.updateMastery(id, score));
    }

    @PutMapping("/{id}/status")
    public Result<LearningPhase> updateStatus(@PathVariable Long id, @RequestParam String status) {
        return Result.ok(phaseService.updateStatus(id, status));
    }
}
