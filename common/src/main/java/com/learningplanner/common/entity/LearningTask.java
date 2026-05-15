package com.learningplanner.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;

@Data
@TableName("learning_task")
public class LearningTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long phaseId;
    private String taskName;
    private String taskDesc;
    private String status;
    private LocalDate deadline;
    private Integer progress;
    private Integer priority;
}
