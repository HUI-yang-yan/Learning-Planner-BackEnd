package com.learningplanner.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("reminder_record")
public class ReminderRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long userId;
    private LocalDateTime remindTime;
    private String remindType;
    private String status;
}
