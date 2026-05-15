package com.learningplanner.reminder.feign;

import com.learningplanner.common.dto.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "task-service")
public interface TaskFeignClient {

    @GetMapping("/api/task/overdue")
    Result<Map<String, Object>> getOverdueTasks(@RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "100") int size);
}
