package com.learningplanner.reminder.config;

import com.learningplanner.reminder.job.*;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {

    @Bean
    public JobDetail dailyReminderJobDetail() {
        return JobBuilder.newJob(DailyReminderJob.class)
                .withIdentity("dailyReminderJob").storeDurably().build();
    }

    @Bean
    public Trigger dailyReminderTrigger() {
        return TriggerBuilder.newTrigger().forJob(dailyReminderJobDetail())
                .withIdentity("dailyReminderTrigger")
                .withSchedule(CronScheduleBuilder.dailyAtHourAndMinute(20, 0)).build();
    }

    @Bean
    public JobDetail taskOverdueJobDetail() {
        return JobBuilder.newJob(TaskOverdueJob.class)
                .withIdentity("taskOverdueJob").storeDurably().build();
    }

    @Bean
    public Trigger taskOverdueTrigger() {
        return TriggerBuilder.newTrigger().forJob(taskOverdueJobDetail())
                .withIdentity("taskOverdueTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 * * * ?")).build();
    }

    @Bean
    public JobDetail inactivityJobDetail() {
        return JobBuilder.newJob(InactivityReminderJob.class)
                .withIdentity("inactivityReminderJob").storeDurably().build();
    }

    @Bean
    public Trigger inactivityTrigger() {
        return TriggerBuilder.newTrigger().forJob(inactivityJobDetail())
                .withIdentity("inactivityReminderTrigger")
                .withSchedule(CronScheduleBuilder.dailyAtHourAndMinute(10, 0)).build();
    }

    @Bean
    public JobDetail weeklyReportJobDetail() {
        return JobBuilder.newJob(WeeklyReportJob.class)
                .withIdentity("weeklyReportJob").storeDurably().build();
    }

    @Bean
    public Trigger weeklyReportTrigger() {
        return TriggerBuilder.newTrigger().forJob(weeklyReportJobDetail())
                .withIdentity("weeklyReportTrigger")
                .withSchedule(CronScheduleBuilder.weeklyOnDayAndHourAndMinute(DateBuilder.MONDAY, 9, 0)).build();
    }
}
