package com.learningplanner.planner.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.learningplanner.common.entity.LearningGoal;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LearningGoalMapper extends BaseMapper<LearningGoal> {
}
