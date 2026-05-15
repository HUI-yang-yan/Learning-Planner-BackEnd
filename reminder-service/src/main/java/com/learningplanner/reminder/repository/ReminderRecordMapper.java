package com.learningplanner.reminder.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.learningplanner.common.entity.ReminderRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ReminderRecordMapper extends BaseMapper<ReminderRecord> {
}
