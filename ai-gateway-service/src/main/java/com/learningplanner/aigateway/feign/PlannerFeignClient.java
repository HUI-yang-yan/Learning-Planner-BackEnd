package com.learningplanner.aigateway.feign;

import com.learningplanner.common.dto.AIResultRequest;
import com.learningplanner.common.dto.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "planner-service")
public interface PlannerFeignClient {

    @PostMapping("/api/planner/goals/{goalId}/ai-result")
    Result<Void> saveAIResult(@PathVariable Long goalId, @RequestBody AIResultRequest request);
}
