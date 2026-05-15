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
import java.util.Map;

@Slf4j
@Component
public class DailyReminderJob extends QuartzJobBean {

    private final ReminderRecordService reminderService;
    private final RedisTemplate<String, String> redisTemplate;

    public DailyReminderJob(ReminderRecordService reminderService,
                            RedisTemplate<String, String> redisTemplate) {
        this.reminderService = reminderService;
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        log.info("[DailyReminder] Scanning for active users...");
        // Query users with active goals - for now create reminder for user 1 (demo)
        // In production, iterate all active users
        try {
            // Broadcast daily learning reminder via Redis → WebSocket
            String message = "Time to start today's learning!";
            redisTemplate.convertAndSend("ai-stream-channel",
                    "{\"goalId\":0,\"type\":\"DAILY_REMINDER\",\"content\":\"" + message + "\"}");
            log.info("[DailyReminder] Daily reminder broadcasted");
        } catch (Exception e) {
            log.error("[DailyReminder] Failed: {}", e.getMessage());
        }
    }
}
