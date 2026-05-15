package com.learningplanner.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("learning_phase")
public class LearningPhase {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long goalId;
    private String phaseName;
    private Integer phaseOrder;
    private String phaseDesc;
    private Integer masteryScore;
    private Integer estimatedDays;
    private String status;
}
