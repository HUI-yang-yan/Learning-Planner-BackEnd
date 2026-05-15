package com.learningplanner.reminder.job;

import com.learningplanner.common.dto.Result;
import com.learningplanner.reminder.feign.TaskFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class TaskOverdueJob extends QuartzJobBean {

    private final TaskFeignClient taskFeignClient;

    public TaskOverdueJob(TaskFeignClient taskFeignClient) {
        this.taskFeignClient = taskFeignClient;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        Result<Map<String, Object>> result = taskFeignClient.getOverdueTasks(1, 100);
        if (result.getCode() == 200 && result.getData() != null) {
            int total = ((Number) result.getData().get("total")).intValue();
            if (total > 0) {
                log.info("[TaskOverdue] 检测到 {} 个超时任务", total);
            }
        }
    }
}
