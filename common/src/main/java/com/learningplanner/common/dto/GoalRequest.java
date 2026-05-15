package com.learningplanner.common.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GoalRequest {
    @NotBlank(message = "目标名称不能为空")
    private String goalName;
    private String goalDesc;
    private String estimatedDuration;
}
