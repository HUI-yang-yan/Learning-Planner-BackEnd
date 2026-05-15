package com.learningplanner.reminder;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.learningplanner.reminder", "com.learningplanner.common"})
@EnableDiscoveryClient
@EnableScheduling
@MapperScan("com.learningplanner.reminder.repository")
public class ReminderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReminderServiceApplication.class, args);
    }
}
