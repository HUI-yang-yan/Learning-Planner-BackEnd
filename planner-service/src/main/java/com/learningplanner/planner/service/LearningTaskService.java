package com.learningplanner.planner.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.learningplanner.common.entity.LearningTask;
import com.learningplanner.planner.repository.LearningTaskMapper;
import org.springframework.stereotype.Service;

@Service
public class LearningTaskService extends ServiceImpl<LearningTaskMapper, LearningTask> {
}
