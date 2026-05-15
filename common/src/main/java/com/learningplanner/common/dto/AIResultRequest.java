package com.learningplanner.common.dto;

import lombok.Data;
import java.util.List;

@Data
public class AIResultRequest {
    private Long goalId;
    private String analysisResult;
    private List<PhaseDTO> phases;

    @Data
    public static class PhaseDTO {
        private String phaseName;
        private Integer phaseOrder;
        private List<TaskDTO> tasks;

        @Data
        public static class TaskDTO {
            private String taskName;
            private String taskDesc;
            private String deadline;
        }
    }
}
