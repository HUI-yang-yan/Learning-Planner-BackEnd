package com.learningplanner.task.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.learningplanner.common.dto.TaskRequest;
import com.learningplanner.common.entity.LearningTask;
import com.learningplanner.task.repository.LearningTaskMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class LearningTaskService extends ServiceImpl<LearningTaskMapper, LearningTask> {

    public LearningTask create(TaskRequest request) {
        LearningTask task = new LearningTask();
        task.setPhaseId(request.getPhaseId());
        task.setTaskName(request.getTaskName());
        task.setTaskDesc(request.getTaskDesc());
        task.setDeadline(request.getDeadline());
        task.setStatus("PENDING");
        task.setProgress(0);
        save(task);
        return task;
    }

    public Page<LearningTask> pageByPhase(Long phaseId, int page, int size) {
        return page(new Page<>(page, size),
                new LambdaQueryWrapper<LearningTask>()
                        .eq(LearningTask::getPhaseId, phaseId));
    }

    public LearningTask updateStatus(Long id, String status) {
        LearningTask task = getById(id);
        if (task != null) {
            task.setStatus(status);
            if ("COMPLETED".equals(status)) {
                task.setProgress(100);
            }
            updateById(task);
        }
        return task;
    }

    public LearningTask updateProgress(Long id, Integer progress) {
        LearningTask task = getById(id);
        if (task != null) {
            task.setProgress(progress);
            if (progress >= 100) {
                task.setStatus("COMPLETED");
            } else if (progress > 0) {
                task.setStatus("IN_PROGRESS");
            }
            updateById(task);
        }
        return task;
    }

    public Page<LearningTask> pageOverdue(int page, int size) {
        return page(new Page<>(page, size),
                new LambdaQueryWrapper<LearningTask>()
                        .lt(LearningTask::getDeadline, LocalDateTime.now())
                        .ne(LearningTask::getStatus, "COMPLETED"));
    }
}
