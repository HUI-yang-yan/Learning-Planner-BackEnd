package com.learningplanner.reminder.job;

import com.learningplanner.common.dto.Result;
import com.learningplanner.common.entity.LearningTask;
import com.learningplanner.reminder.feign.TaskFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class TaskOverdueJob extends QuartzJobBean {

    private final TaskFeignClient taskFeignClient;
    private final RedisTemplate<String, String> redisTemplate;

    public TaskOverdueJob(TaskFeignClient taskFeignClient,
                          RedisTemplate<String, String> redisTemplate) {
        this.taskFeignClient = taskFeignClient;
        this.redisTemplate = redisTemplate;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        try {
            Result<Map<String, Object>> result = taskFeignClient.getOverdueTasks(1, 100);
            if (result.getCode() == 200 && result.getData() != null) {
                int total = ((Number) result.getData().get("total")).intValue();
                if (total > 0) {
                    List<Map<String, Object>> tasks = (List<Map<String, Object>>) result.getData().get("records");
                    for (Map<String, Object> task : tasks) {
                        String taskName = (String) task.get("taskName");
                        String message = "Task overdue: " + taskName;
                        redisTemplate.convertAndSend("ai-stream-channel",
                                "{\"goalId\":0,\"type\":\"TASK_OVERDUE\",\"content\":\"" + message + "\"}");
                    }
                    log.info("[TaskOverdue] Found {} overdue tasks", total);
                }
            }
        } catch (Exception e) {
            log.error("[TaskOverdue] Failed: {}", e.getMessage());
        }
    }
}
