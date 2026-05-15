package com.learningplanner.reminder.controller;

import com.learningplanner.common.dto.Result;
import com.learningplanner.common.entity.ReminderRecord;
import com.learningplanner.reminder.service.ReminderRecordService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/reminders")
public class ReminderRecordController {

    private final ReminderRecordService reminderService;

    public ReminderRecordController(ReminderRecordService reminderService) {
        this.reminderService = reminderService;
    }

    @PostMapping
    public Result<ReminderRecord> create(@RequestParam Long taskId,
                                          @RequestParam Long userId,
                                          @RequestParam LocalDateTime remindTime,
                                          @RequestParam(defaultValue = "DAILY") String remindType) {
        return Result.ok(reminderService.create(taskId, userId, remindTime, remindType));
    }

    @GetMapping
    public Result<?> pageByUser(@RequestParam Long userId,
                                 @RequestParam(defaultValue = "1") int page,
                                 @RequestParam(defaultValue = "10") int size) {
        return Result.ok(reminderService.pageByUser(userId, page, size));
    }

    @GetMapping("/task")
    public Result<?> pageByTask(@RequestParam Long taskId,
                                 @RequestParam(defaultValue = "1") int page,
                                 @RequestParam(defaultValue = "10") int size) {
        return Result.ok(reminderService.pageByTask(taskId, page, size));
    }

    @PutMapping("/{id}/send")
    public Result<Void> markSent(@PathVariable Long id) {
        reminderService.markSent(id);
        return Result.ok();
    }
}
