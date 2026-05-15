package com.learningplanner.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TaskRequest {
    @NotNull(message = "阶段ID不能为空")
    private Long phaseId;
    @NotBlank(message = "任务名称不能为空")
    private String taskName;
    private String taskDesc;
    private LocalDateTime deadline;
}
