package com.learningplanner.aigateway.feign;

import com.learningplanner.common.dto.AIResultRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "python-ai-service", url = "${ai.python.service.url:http://localhost:8000}")
public interface PythonAIClient {

    @PostMapping("/goal/analyze")
    Map<String, Object> analyzeGoal(@RequestBody Map<String, Object> request);

    @PostMapping("/roadmap/generate")
    AIResultRequest generateRoadmap(@RequestBody Map<String, Object> request);

    @PostMapping("/task/generate")
    Map<String, Object> generateTasks(@RequestBody Map<String, Object> request);

    @PostMapping("/mastery/evaluate")
    Map<String, Object> evaluateMastery(@RequestBody Map<String, Object> request);
}
