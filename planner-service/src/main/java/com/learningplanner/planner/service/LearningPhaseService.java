package com.learningplanner.planner.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.learningplanner.common.dto.PhaseRequest;
import com.learningplanner.common.entity.LearningPhase;
import com.learningplanner.planner.repository.LearningPhaseMapper;
import org.springframework.stereotype.Service;

@Service
public class LearningPhaseService extends ServiceImpl<LearningPhaseMapper, LearningPhase> {

    public LearningPhase create(PhaseRequest request) {
        LearningPhase phase = new LearningPhase();
        phase.setGoalId(request.getGoalId());
        phase.setPhaseName(request.getPhaseName());
        phase.setPhaseOrder(request.getPhaseOrder());
        phase.setMasteryScore(0);
        phase.setStatus("PENDING");
        save(phase);
        return phase;
    }

    public Page<LearningPhase> pageByGoal(Long goalId, int page, int size) {
        return page(new Page<>(page, size),
                new LambdaQueryWrapper<LearningPhase>()
                        .eq(LearningPhase::getGoalId, goalId)
                        .orderByAsc(LearningPhase::getPhaseOrder));
    }

    public LearningPhase updateMastery(Long id, Integer score) {
        LearningPhase phase = getById(id);
        if (phase != null) {
            phase.setMasteryScore(score);
            updateById(phase);
        }
        return phase;
    }

    public LearningPhase updateStatus(Long id, String status) {
        LearningPhase phase = getById(id);
        if (phase != null) {
            phase.setStatus(status);
            updateById(phase);
        }
        return phase;
    }
}
