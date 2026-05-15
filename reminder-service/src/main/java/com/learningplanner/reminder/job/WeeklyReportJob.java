package com.learningplanner.reminder.job;

import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WeeklyReportJob extends QuartzJobBean {

    private final RedisTemplate<String, String> redisTemplate;

    public WeeklyReportJob(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        log.info("[WeeklyReport] Generating weekly learning report...");
        try {
            // In full implementation: aggregate task completion stats for each user
            String report = "Weekly learning report is ready. Check your progress dashboard!";
            redisTemplate.convertAndSend("ai-stream-channel",
                    "{\"goalId\":0,\"type\":\"WEEKLY_REPORT\",\"content\":\"" + report + "\"}");
            log.info("[WeeklyReport] Weekly report broadcasted");
        } catch (Exception e) {
            log.error("[WeeklyReport] Failed: {}", e.getMessage());
        }
    }
}
