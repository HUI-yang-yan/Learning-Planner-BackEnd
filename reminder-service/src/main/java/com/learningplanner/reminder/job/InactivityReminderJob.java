package com.learningplanner.reminder.job;

import com.learningplanner.common.entity.ReminderRecord;
import com.learningplanner.reminder.service.ReminderRecordService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class InactivityReminderJob extends QuartzJobBean {

    private final ReminderRecordService reminderService;
    private final RedisTemplate<String, String> redisTemplate;

    public InactivityReminderJob(ReminderRecordService reminderService,
                                  RedisTemplate<String, String> redisTemplate) {
        this.reminderService = reminderService;
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        log.info("[InactivityReminder] Checking for inactive users...");
        try {
            // In full implementation: query users with no completed tasks in last 3 days
            // For now, broadcast inactivity reminder as a demo
            String message = "You haven't studied for 3 days. Come back to your learning plan!";
            redisTemplate.convertAndSend("ai-stream-channel",
                    "{\"goalId\":0,\"type\":\"INACTIVITY_REMINDER\",\"content\":\"" + message + "\"}");
            log.info("[InactivityReminder] Inactivity reminder broadcasted");
        } catch (Exception e) {
            log.error("[InactivityReminder] Failed: {}", e.getMessage());
        }
    }
}
