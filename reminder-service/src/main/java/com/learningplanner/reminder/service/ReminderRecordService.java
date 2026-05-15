package com.learningplanner.reminder.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.learningplanner.common.entity.ReminderRecord;
import com.learningplanner.reminder.repository.ReminderRecordMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ReminderRecordService extends ServiceImpl<ReminderRecordMapper, ReminderRecord> {

    public ReminderRecord create(Long taskId, Long userId, LocalDateTime remindTime, String remindType) {
        ReminderRecord record = new ReminderRecord();
        record.setTaskId(taskId);
        record.setUserId(userId);
        record.setRemindTime(remindTime);
        record.setRemindType(remindType != null ? remindType : "DAILY");
        record.setStatus("PENDING");
        save(record);
        return record;
    }

    public Page<ReminderRecord> pageByUser(Long userId, int page, int size) {
        return page(new Page<>(page, size),
                new LambdaQueryWrapper<ReminderRecord>()
                        .eq(ReminderRecord::getUserId, userId)
                        .orderByDesc(ReminderRecord::getRemindTime));
    }

    public Page<ReminderRecord> pageByTask(Long taskId, int page, int size) {
        return page(new Page<>(page, size),
                new LambdaQueryWrapper<ReminderRecord>()
                        .eq(ReminderRecord::getTaskId, taskId)
                        .orderByDesc(ReminderRecord::getRemindTime));
    }

    public void markSent(Long id) {
        ReminderRecord record = getById(id);
        if (record != null) {
            record.setStatus("SENT");
            updateById(record);
        }
    }
}
