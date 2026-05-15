package com.learningplanner.planner.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.learningplanner.common.dto.GoalRequest;
import com.learningplanner.common.entity.LearningGoal;
import com.learningplanner.planner.mq.GoalAnalysisProducer;
import com.learningplanner.planner.repository.LearningGoalMapper;
import org.springframework.stereotype.Service;

@Service
public class LearningGoalService extends ServiceImpl<LearningGoalMapper, LearningGoal> {

    private final GoalAnalysisProducer goalAnalysisProducer;

    public LearningGoalService(GoalAnalysisProducer goalAnalysisProducer) {
        this.goalAnalysisProducer = goalAnalysisProducer;
    }

    public LearningGoal create(GoalRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        LearningGoal goal = new LearningGoal();
        goal.setUserId(userId);
        goal.setGoalName(request.getGoalName());
        goal.setGoalDesc(request.getGoalDesc());
        goal.setEstimatedDuration(request.getEstimatedDuration());
        goal.setStatus("ANALYZING");
        save(goal);
        goalAnalysisProducer.sendGoalAnalysis(goal.getId(), userId,
                request.getGoalName(), request.getGoalDesc());
        return goal;
    }

    public Page<LearningGoal> pageByUser(int page, int size) {
        Long userId = StpUtil.getLoginIdAsLong();
        return page(new Page<>(page, size),
                new LambdaQueryWrapper<LearningGoal>()
                        .eq(LearningGoal::getUserId, userId)
                        .orderByDesc(LearningGoal::getCreateTime));
    }

    public LearningGoal updateStatus(Long id, String status) {
        LearningGoal goal = getById(id);
        if (goal != null) {
            goal.setStatus(status);
            updateById(goal);
        }
        return goal;
    }
}
