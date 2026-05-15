package com.learningplanner.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("learning_goal")
public class LearningGoal {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String goalName;
    private String goalDesc;
    private String status;
    private String estimatedDuration;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
