package com.learningplanner.planner.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.learningplanner.common.entity.LearningTask;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LearningTaskMapper extends BaseMapper<LearningTask> {
}
