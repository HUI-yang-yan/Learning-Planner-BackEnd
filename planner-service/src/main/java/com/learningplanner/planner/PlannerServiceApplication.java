package com.learningplanner.planner;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {"com.learningplanner.planner", "com.learningplanner.common"})
@EnableDiscoveryClient
@EnableFeignClients
@MapperScan("com.learningplanner.planner.repository")
public class PlannerServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PlannerServiceApplication.class, args);
    }
}
