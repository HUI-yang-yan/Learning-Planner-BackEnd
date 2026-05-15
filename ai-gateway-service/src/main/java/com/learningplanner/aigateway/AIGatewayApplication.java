package com.learningplanner.aigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = "com.learningplanner")
@EnableFeignClients(basePackages = "com.learningplanner.aigateway.feign")
public class AIGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(AIGatewayApplication.class, args);
    }
}
