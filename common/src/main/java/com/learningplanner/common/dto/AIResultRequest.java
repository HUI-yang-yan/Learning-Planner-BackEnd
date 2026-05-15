package com.learningplanner.common.dto;

import lombok.Data;
import java.util.List;

@Data
public class AIResultRequest {
    private Long goalId;
    private String goalType;
    private String difficulty;
    private String estimatedDuration;
    private List<String> requiredSkills;
    private List<PhaseDTO> phases;

    @Data
    public static class PhaseDTO {
        private String phaseName;
        private Integer phaseOrder;
        private String phaseDesc;
        private Integer estimatedDays;
        private List<TaskDTO> tasks;

        @Data
        public static class TaskDTO {
            private String taskName;
            private String taskDesc;
            private Integer priority;
            private Integer estimatedHours;
        }
    }
}
