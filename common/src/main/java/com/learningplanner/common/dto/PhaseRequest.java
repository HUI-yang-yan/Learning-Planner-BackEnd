package com.learningplanner.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PhaseRequest {
    @NotNull(message = "目标ID不能为空")
    private Long goalId;
    @NotBlank(message = "阶段名称不能为空")
    private String phaseName;
    @NotNull(message = "阶段顺序不能为空")
    private Integer phaseOrder;
}
