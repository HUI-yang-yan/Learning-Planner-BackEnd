package com.learningplanner.reminder.controller;

import com.learningplanner.common.dto.Result;
import com.learningplanner.common.entity.ReminderRecord;
import com.learningplanner.reminder.service.ReminderRecordService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/reminder")
public class ReminderRecordController {

    private final ReminderRecordService reminderService;

    public ReminderRecordController(ReminderRecordService reminderService) {
        this.reminderService = reminderService;
    }

    @PostMapping
    public Result<ReminderRecord> create(@RequestParam Long taskId,
                                          @RequestParam LocalDateTime remindTime) {
        return Result.ok(reminderService.create(taskId, remindTime));
    }

    @GetMapping("/page")
    public Result<?> page(@RequestParam Long taskId,
                          @RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int size) {
        return Result.ok(reminderService.pageByTask(taskId, page, size));
    }
}
