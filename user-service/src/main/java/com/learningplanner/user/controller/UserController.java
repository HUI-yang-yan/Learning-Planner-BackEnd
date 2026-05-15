package com.learningplanner.user.controller;

import com.learningplanner.common.dto.LoginRequest;
import com.learningplanner.common.dto.RegisterRequest;
import com.learningplanner.common.dto.Result;
import com.learningplanner.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public Result<String> login(@RequestBody @Valid LoginRequest request) {
        return Result.ok(userService.login(request));
    }

    @PostMapping("/register")
    public Result<Void> register(@RequestBody @Valid RegisterRequest request) {
        userService.register(request);
        return Result.ok();
    }

    @GetMapping("/info")
    public Result<?> info() {
        return Result.ok(userService.getLoginUser());
    }
}
