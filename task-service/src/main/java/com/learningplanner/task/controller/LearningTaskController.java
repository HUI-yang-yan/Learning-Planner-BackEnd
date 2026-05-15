package com.learningplanner.task.controller;

import com.learningplanner.common.dto.Result;
import com.learningplanner.common.dto.TaskRequest;
import com.learningplanner.common.entity.LearningTask;
import com.learningplanner.task.service.LearningTaskService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/task")
public class LearningTaskController {

    private final LearningTaskService taskService;

    public LearningTaskController(LearningTaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public Result<LearningTask> create(@RequestBody @Valid TaskRequest request) {
        return Result.ok(taskService.create(request));
    }

    @GetMapping("/{id}")
    public Result<LearningTask> getById(@PathVariable Long id) {
        return Result.ok(taskService.getById(id));
    }

    @GetMapping("/page")
    public Result<?> page(@RequestParam Long phaseId,
                          @RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int size) {
        return Result.ok(taskService.pageByPhase(phaseId, page, size));
    }

    @GetMapping("/overdue")
    public Result<Map<String, Object>> overdue(@RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "100") int size) {
        return Result.ok(Map.of(
                "total", taskService.pageOverdue(page, size).getTotal(),
                "records", taskService.pageOverdue(page, size).getRecords()
        ));
    }

    @PutMapping("/{id}")
    public Result<LearningTask> update(@PathVariable Long id, @RequestBody TaskRequest request) {
        LearningTask task = taskService.getById(id);
        if (task != null) {
            task.setTaskName(request.getTaskName());
            task.setTaskDesc(request.getTaskDesc());
            task.setDeadline(request.getDeadline());
            taskService.updateById(task);
        }
        return Result.ok(task);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        taskService.removeById(id);
        return Result.ok();
    }

    @PutMapping("/{id}/status")
    public Result<LearningTask> updateStatus(@PathVariable Long id, @RequestParam String status) {
        return Result.ok(taskService.updateStatus(id, status));
    }

    @PutMapping("/{id}/progress")
    public Result<LearningTask> updateProgress(@PathVariable Long id, @RequestParam Integer progress) {
        return Result.ok(taskService.updateProgress(id, progress));
    }
}
