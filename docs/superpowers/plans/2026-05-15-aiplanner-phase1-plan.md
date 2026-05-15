# AIPlanner Phase 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 跑通核心闭环 - 用户提交 Goal → Python LangGraph AI 生成学习路线 → Java 保存并展示

**Architecture:** 保留现有 8 模块 Java 微服务骨架，新建 Python AI 服务，通过 RabbitMQ 异步通信。Java 侧修复实体字段、完善业务逻辑、修复 Gateway JWT 过滤器；Python 侧从零搭建 FastAPI + LangGraph + pika 项目。

**Tech Stack:** Spring Boot 3.4.7, Spring Cloud Gateway, MyBatis Plus 3.5.10, Sa-Token 1.44.0, RabbitMQ, Python 3.12, FastAPI, LangGraph, pika, MySQL 8, Redis

---

## 文件结构总览

```
Learning-Planner-Backend/
├── common/src/main/java/com/learningplanner/common/
│   ├── entity/
│   │   ├── User.java              (无需改动)
│   │   ├── LearningGoal.java      (无需改动)
│   │   ├── LearningPhase.java     (添加 phaseDesc, estimatedDays)
│   │   ├── LearningTask.java      (deadline → LocalDate, 添加 priority)
│   │   └── ReminderRecord.java    (添加 userId, remindType)
│   └── dto/
│       ├── AIResultRequest.java   (匹配 Python 返回结构)
│       └── GoalRequest.java       (无需改动)
├── user-service/                   (基本可用, 微调)
├── planner-service/                (核心改动: MQ发送, AI结果接收)
├── ai-gateway-service/             (完善 MQ Consumer 保存逻辑)
├── gateway/                        (修复 JWT 过滤器)
├── ai-planner-service/             (新建 Python 项目)
└── sql/                            (新建 DDL)
```

---

### Task 1: SQL 建表脚本

**Files:**
- Create: `sql/V1__init_tables.sql`

- [ ] **Step 1: 编写建表 SQL**

```sql
-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(50) NOT NULL UNIQUE,
    `password` VARCHAR(255) NOT NULL,
    `avatar` VARCHAR(255),
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 学习目标表
CREATE TABLE IF NOT EXISTS `learning_goal` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `goal_name` VARCHAR(100) NOT NULL,
    `goal_desc` TEXT,
    `status` VARCHAR(20) NOT NULL DEFAULT 'ANALYZING',
    `estimated_duration` VARCHAR(50),
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 学习阶段表
CREATE TABLE IF NOT EXISTS `learning_phase` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `goal_id` BIGINT NOT NULL,
    `phase_name` VARCHAR(100) NOT NULL,
    `phase_order` INT NOT NULL,
    `phase_desc` TEXT,
    `mastery_score` INT DEFAULT 0,
    `estimated_days` INT DEFAULT 0,
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    INDEX `idx_goal_id` (`goal_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 学习任务表
CREATE TABLE IF NOT EXISTS `learning_task` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `phase_id` BIGINT NOT NULL,
    `task_name` VARCHAR(200) NOT NULL,
    `task_desc` TEXT,
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    `deadline` DATE,
    `progress` INT DEFAULT 0,
    `priority` INT DEFAULT 1,
    INDEX `idx_phase_id` (`phase_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 提醒记录表
CREATE TABLE IF NOT EXISTS `reminder_record` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `task_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `remind_time` DATETIME NOT NULL,
    `remind_type` VARCHAR(20) NOT NULL DEFAULT 'DAILY',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    INDEX `idx_user_remind` (`user_id`, `remind_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [ ] **Step 2: 执行 SQL**

```bash
# 在 MySQL 中执行 (数据库已存在: ai_platform)
mysql -h 192.168.171.135 -u root -p123456 ai_platform < sql/V1__init_tables.sql
```

- [ ] **Step 3: 提交**

```bash
git add sql/V1__init_tables.sql
git commit -m "feat: add database initialization SQL"
```

---

### Task 2: 修复实体类字段

**Files:**
- Modify: `common/src/main/java/com/learningplanner/common/entity/LearningPhase.java`
- Modify: `common/src/main/java/com/learningplanner/common/entity/LearningTask.java`
- Modify: `common/src/main/java/com/learningplanner/common/entity/ReminderRecord.java`

- [ ] **Step 1: 修改 LearningPhase - 添加 phaseDesc 和 estimatedDays**

`common/src/main/java/com/learningplanner/common/entity/LearningPhase.java`:
```java
package com.learningplanner.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("learning_phase")
public class LearningPhase {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long goalId;
    private String phaseName;
    private Integer phaseOrder;
    private String phaseDesc;
    private Integer masteryScore;
    private Integer estimatedDays;
    private String status;
}
```

- [ ] **Step 2: 修改 LearningTask - deadline 类型改为 LocalDate，添加 priority**

`common/src/main/java/com/learningplanner/common/entity/LearningTask.java`:
```java
package com.learningplanner.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;

@Data
@TableName("learning_task")
public class LearningTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long phaseId;
    private String taskName;
    private String taskDesc;
    private String status;
    private LocalDate deadline;
    private Integer progress;
    private Integer priority;
}
```

- [ ] **Step 3: 修改 ReminderRecord - 添加 userId 和 remindType**

`common/src/main/java/com/learningplanner/common/entity/ReminderRecord.java`:
```java
package com.learningplanner.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("reminder_record")
public class ReminderRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long userId;
    private LocalDateTime remindTime;
    private String remindType;
    private String status;
}
```

- [ ] **Step 4: 验证编译**

```bash
mvn compile -pl common
```
Expected: BUILD SUCCESS

- [ ] **Step 5: 提交**

```bash
git add common/src/main/java/com/learningplanner/common/entity/
git commit -m "fix: add missing entity fields matching SQL schema"
```

---

### Task 3: 完善用户注册/登录

**Files:**
- Modify: `user-service/src/main/java/com/learningplanner/user/service/UserService.java:30` (MD5 → BCrypt)

- [ ] **Step 1: 将密码加密从 MD5 改为 BCrypt**

`user-service/src/main/java/com/learningplanner/user/service/UserService.java`:
```java
package com.learningplanner.user.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.learningplanner.common.dto.LoginRequest;
import com.learningplanner.common.dto.RegisterRequest;
import com.learningplanner.common.entity.User;
import com.learningplanner.user.repository.UserMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService extends ServiceImpl<UserMapper, User> {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public String login(LoginRequest request) {
        // 先按用户名查用户，再校验密码
        User user = getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername()));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }
        StpUtil.login(user.getId());
        return StpUtil.getTokenValue();
    }

    public void register(RegisterRequest request) {
        long count = count(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername()));
        if (count > 0) {
            throw new RuntimeException("用户名已存在");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        save(user);
    }

    public User getLoginUser() {
        return getById(StpUtil.getLoginIdAsLong());
    }
}
```

- [ ] **Step 2: 验证编译**

```bash
mvn compile -pl user-service
```
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add user-service/src/main/java/com/learningplanner/user/service/UserService.java
git commit -m "feat: upgrade password encoding to BCrypt"
```

---

### Task 4: 修复 Gateway JWT 过滤器

**Files:**
- Modify: `gateway/src/main/java/com/learningplanner/gateway/filter/JwtAuthFilter.java`

Sa-Token Reactor 中不能直接用 `StpUtil.isLogin()`，需要用 `StpUtil.getLoginId()` 配合 try-catch。

`gateway/src/main/java/com/learningplanner/gateway/filter/JwtAuthFilter.java`:
```java
package com.learningplanner.gateway.filter;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private static final List<String> EXCLUDE_PATHS = List.of(
            "/api/user/login", "/api/user/register"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        for (String exclude : EXCLUDE_PATHS) {
            if (path.startsWith(exclude)) {
                return chain.filter(exchange);
            }
        }

        // Sa-Token Reactor: 从 Header 读取 Token 并校验
        try {
            String token = exchange.getRequest().getHeaders().getFirst("Authorization");
            if (token != null && !token.isBlank()) {
                Object loginId = StpUtil.getLoginIdByToken(token);
                if (loginId != null) {
                    return chain.filter(exchange);
                }
            }
        } catch (NotLoginException ignored) {
        }

        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
```

- [ ] **Step 2: 验证编译**

```bash
mvn compile -pl gateway
```
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add gateway/src/main/java/com/learningplanner/gateway/filter/JwtAuthFilter.java
git commit -m "fix: use Sa-Token Reactor API in gateway JWT filter"
```

---

### Task 5: Planner Service - 添加 MQ 支持并触发 AI 分析

**Files:**
- Modify: `planner-service/pom.xml` (添加 RabbitMQ 依赖)
- Create: `planner-service/src/main/java/com/learningplanner/planner/config/RabbitMQConfig.java`
- Create: `planner-service/src/main/java/com/learningplanner/planner/mq/GoalAnalysisProducer.java`
- Modify: `planner-service/src/main/java/com/learningplanner/planner/service/LearningGoalService.java:15-24` (发送 MQ)

- [ ] **Step 1: planner-service pom.xml 添加 RabbitMQ 依赖**

`planner-service/pom.xml`, 在 `<dependencies>` 内添加:
```xml
        <!-- RabbitMQ -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-amqp</artifactId>
        </dependency>
```

- [ ] **Step 2: 在 application.yml 添加 RabbitMQ 配置**

`planner-service/src/main/resources/application.yml`, 追加:
```yaml
  rabbitmq:
    host: 192.168.171.135
    port: 5672
    username: guest
    password: guest
```

- [ ] **Step 3: 创建 RabbitMQ 配置类**

`planner-service/src/main/java/com/learningplanner/planner/config/RabbitMQConfig.java`:
```java
package com.learningplanner.planner.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String AI_EXCHANGE = "ai.exchange";
    public static final String GOAL_ANALYSIS_QUEUE = "goal.analysis.queue";

    @Bean
    public Queue goalAnalysisQueue() {
        return QueueBuilder.durable(GOAL_ANALYSIS_QUEUE).build();
    }

    @Bean
    public DirectExchange aiExchange() {
        return new DirectExchange(AI_EXCHANGE);
    }

    @Bean
    public Binding goalAnalysisBinding() {
        return BindingBuilder.bind(goalAnalysisQueue()).to(aiExchange()).with("goal.analysis");
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
```

- [ ] **Step 4: 创建 MQ Producer**

`planner-service/src/main/java/com/learningplanner/planner/mq/GoalAnalysisProducer.java`:
```java
package com.learningplanner.planner.mq;

import com.learningplanner.planner.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class GoalAnalysisProducer {

    private final RabbitTemplate rabbitTemplate;

    public GoalAnalysisProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendGoalAnalysis(Long goalId, Long userId, String goalName, String goalDesc) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.AI_EXCHANGE, "goal.analysis",
                Map.of("goalId", goalId, "userId", userId,
                       "goalName", goalName, "goalDesc", goalDesc));
    }
}
```

- [ ] **Step 5: 修改 LearningGoalService - 创建后发 MQ**

`planner-service/src/main/java/com/learningplanner/planner/service/LearningGoalService.java`:
```java
package com.learningplanner.planner.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.learningplanner.common.dto.GoalRequest;
import com.learningplanner.common.entity.LearningGoal;
import com.learningplanner.planner.mq.GoalAnalysisProducer;
import com.learningplanner.planner.repository.LearningGoalMapper;
import org.springframework.stereotype.Service;

@Service
public class LearningGoalService extends ServiceImpl<LearningGoalMapper, LearningGoal> {

    private final GoalAnalysisProducer goalAnalysisProducer;

    public LearningGoalService(GoalAnalysisProducer goalAnalysisProducer) {
        this.goalAnalysisProducer = goalAnalysisProducer;
    }

    public LearningGoal create(GoalRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        LearningGoal goal = new LearningGoal();
        goal.setUserId(userId);
        goal.setGoalName(request.getGoalName());
        goal.setGoalDesc(request.getGoalDesc());
        goal.setEstimatedDuration(request.getEstimatedDuration());
        goal.setStatus("ANALYZING");
        save(goal);
        // 发 MQ 触发 Python AI 分析
        goalAnalysisProducer.sendGoalAnalysis(goal.getId(), userId,
                request.getGoalName(), request.getGoalDesc());
        return goal;
    }

    public Page<LearningGoal> pageByUser(int page, int size) {
        Long userId = StpUtil.getLoginIdAsLong();
        return page(new Page<>(page, size),
                new LambdaQueryWrapper<LearningGoal>()
                        .eq(LearningGoal::getUserId, userId)
                        .orderByDesc(LearningGoal::getCreateTime));
    }

    public LearningGoal updateStatus(Long id, String status) {
        LearningGoal goal = getById(id);
        if (goal != null) {
            goal.setStatus(status);
            updateById(goal);
        }
        return goal;
    }
}
```

- [ ] **Step 6: 验证编译**

```bash
mvn compile -pl planner-service
```
Expected: BUILD SUCCESS

- [ ] **Step 7: 提交**

```bash
git add planner-service/
git commit -m "feat: add MQ producer to trigger AI analysis on goal creation"
```

---

### Task 6: 对齐 Controller 路由路径

**Files:**
- Modify: `planner-service/src/main/java/com/learningplanner/planner/controller/LearningGoalController.java` (路径改为 `/api/planner/goals`)

- [ ] **Step 1: 修改 Controller 路径**

`planner-service/src/main/java/com/learningplanner/planner/controller/LearningGoalController.java`:
```java
package com.learningplanner.planner.controller;

import com.learningplanner.common.dto.GoalRequest;
import com.learningplanner.common.dto.Result;
import com.learningplanner.common.entity.LearningGoal;
import com.learningplanner.planner.service.LearningGoalService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/planner/goals")
public class LearningGoalController {

    private final LearningGoalService goalService;

    public LearningGoalController(LearningGoalService goalService) {
        this.goalService = goalService;
    }

    @PostMapping
    public Result<LearningGoal> create(@RequestBody @Valid GoalRequest request) {
        return Result.ok(goalService.create(request));
    }

    @GetMapping("/{id}")
    public Result<LearningGoal> getById(@PathVariable Long id) {
        return Result.ok(goalService.getById(id));
    }

    @GetMapping
    public Result<?> page(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int size) {
        return Result.ok(goalService.pageByUser(page, size));
    }

    @PutMapping("/{id}")
    public Result<LearningGoal> update(@PathVariable Long id, @RequestBody GoalRequest request) {
        LearningGoal goal = goalService.getById(id);
        if (goal != null) {
            goal.setGoalName(request.getGoalName());
            goal.setGoalDesc(request.getGoalDesc());
            goal.setEstimatedDuration(request.getEstimatedDuration());
            goalService.updateById(goal);
        }
        return Result.ok(goal);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        goalService.removeById(id);
        return Result.ok();
    }

    @PutMapping("/{id}/status")
    public Result<LearningGoal> updateStatus(@PathVariable Long id, @RequestParam String status) {
        return Result.ok(goalService.updateStatus(id, status));
    }
}
```

- [ ] **Step 2: 更新 Gateway 路由**

`gateway/src/main/java/com/learningplanner/gateway/config/GatewayRoutesConfig.java`:
```java
.route("planner-service", r -> r
    .path("/api/planner/**")
    .uri("lb://planner-service"))
```

- [ ] **Step 3: 验证编译**

```bash
mvn compile -pl planner-service,gateway
```
Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add planner-service/ gateway/
git commit -m "fix: align controller paths with gateway routes"
```

---

### Task 7: AI Gateway Service - 完善结果消费

**Files:**
- Modify: `ai-gateway-service/src/main/java/com/learningplanner/aigateway/mq/AIResultConsumer.java`
- Create: `ai-gateway-service/src/main/java/com/learningplanner/aigateway/feign/PlannerFeignClient.java`

- [ ] **Step 1: 创建 PlannerFeignClient**

`ai-gateway-service/src/main/java/com/learningplanner/aigateway/feign/PlannerFeignClient.java`:
```java
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
```

- [ ] **Step 2: 在 AI Gateway 启动类添加 Feign 扫描**

`ai-gateway-service/src/main/java/com/learningplanner/aigateway/AIGatewayApplication.java`:
```java
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
```

- [ ] **Step 3: 完善 AIResultConsumer - 调用 Feign 保存结果**

`ai-gateway-service/src/main/java/com/learningplanner/aigateway/mq/AIResultConsumer.java`:
```java
package com.learningplanner.aigateway.mq;

import com.learningplanner.aigateway.config.RabbitMQConfig;
import com.learningplanner.aigateway.feign.PlannerFeignClient;
import com.learningplanner.common.dto.AIResultRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AIResultConsumer {

    private final PlannerFeignClient plannerFeignClient;

    public AIResultConsumer(PlannerFeignClient plannerFeignClient) {
        this.plannerFeignClient = plannerFeignClient;
    }

    @RabbitListener(queues = RabbitMQConfig.AI_RESULT_QUEUE)
    public void handleAIResult(AIResultRequest result) {
        log.info("[AI Result] 收到AI生成结果: goalId={}, phases={}",
                result.getGoalId(),
                result.getPhases() != null ? result.getPhases().size() : 0);
        try {
            plannerFeignClient.saveAIResult(result.getGoalId(), result);
            log.info("[AI Result] 结果已保存到 planner-service: goalId={}", result.getGoalId());
        } catch (Exception e) {
            log.error("[AI Result] 保存失败: goalId={}", result.getGoalId(), e);
            throw e; // 抛出异常触发 MQ 重试
        }
    }
}
```

- [ ] **Step 4: 验证编译**

```bash
mvn compile -pl ai-gateway-service
```
Expected: BUILD SUCCESS

- [ ] **Step 5: 提交**

```bash
git add ai-gateway-service/
git commit -m "feat: complete AI result consumer with Feign save to planner"
```

---

### Task 8: Planner Service - 接收 AI 结果并保存路线

**Files:**
- Create: `planner-service/src/main/java/com/learningplanner/planner/service/LearningPhaseService.java`
- Create: `planner-service/src/main/java/com/learningplanner/planner/service/LearningTaskService.java`
- Modify: `planner-service/src/main/java/com/learningplanner/planner/controller/LearningGoalController.java` (添加 `/api/planner/goals/{id}/ai-result` 端点)

- [ ] **Step 1: 创建 LearningPhaseService**

`planner-service/src/main/java/com/learningplanner/planner/service/LearningPhaseService.java`:
```java
package com.learningplanner.planner.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.learningplanner.common.entity.LearningPhase;
import com.learningplanner.planner.repository.LearningPhaseMapper;
import org.springframework.stereotype.Service;

@Service
public class LearningPhaseService extends ServiceImpl<LearningPhaseMapper, LearningPhase> {
}
```

- [ ] **Step 2: 创建 LearningTaskService**

`planner-service/src/main/java/com/learningplanner/planner/service/LearningTaskService.java`:
```java
package com.learningplanner.planner.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.learningplanner.common.entity.LearningTask;
import com.learningplanner.planner.repository.LearningTaskMapper;
import org.springframework.stereotype.Service;

@Service
public class LearningTaskService extends ServiceImpl<LearningTaskMapper, LearningTask> {
}
```

- [ ] **Step 3: 创建 LearningPhaseMapper**

`planner-service/src/main/java/com/learningplanner/planner/repository/LearningPhaseMapper.java`:
```java
package com.learningplanner.planner.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.learningplanner.common.entity.LearningPhase;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LearningPhaseMapper extends BaseMapper<LearningPhase> {
}
```

- [ ] **Step 4: 创建 LearningTaskMapper**

`planner-service/src/main/java/com/learningplanner/planner/repository/LearningTaskMapper.java`:
```java
package com.learningplanner.planner.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.learningplanner.common.entity.LearningTask;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LearningTaskMapper extends BaseMapper<LearningTask> {
}
```

- [ ] **Step 5: 在 LearningGoalController 添加接收 AI 结果的端点**

在 `planner-service/src/main/java/com/learningplanner/planner/controller/LearningGoalController.java` 末尾添加:
```java
    private final LearningPhaseService phaseService;
    private final LearningTaskService taskService;

    // 修改构造器
    public LearningGoalController(LearningGoalService goalService,
                                   LearningPhaseService phaseService,
                                   LearningTaskService taskService) {
        this.goalService = goalService;
        this.phaseService = phaseService;
        this.taskService = taskService;
    }

    @PostMapping("/{id}/ai-result")
    public Result<Void> receiveAIResult(@PathVariable Long id,
                                         @RequestBody com.learningplanner.common.dto.AIResultRequest request) {
        LearningGoal goal = goalService.getById(id);
        if (goal == null) {
            return Result.fail(404, "目标不存在");
        }
        // 保存阶段
        if (request.getPhases() != null) {
            for (var phaseDTO : request.getPhases()) {
                LearningPhase phase = new LearningPhase();
                phase.setGoalId(id);
                phase.setPhaseName(phaseDTO.getPhaseName());
                phase.setPhaseOrder(phaseDTO.getPhaseOrder());
                phase.setPhaseDesc(phaseDTO.getPhaseDesc());
                phase.setEstimatedDays(phaseDTO.getEstimatedDays());
                phase.setStatus("PENDING");
                phaseService.save(phase);

                // 保存任务
                if (phaseDTO.getTasks() != null) {
                    for (var taskDTO : phaseDTO.getTasks()) {
                        LearningTask task = new LearningTask();
                        task.setPhaseId(phase.getId());
                        task.setTaskName(taskDTO.getTaskName());
                        task.setTaskDesc(taskDTO.getTaskDesc());
                        task.setPriority(taskDTO.getPriority());
                        task.setStatus("PENDING");
                        taskService.save(task);
                    }
                }
            }
        }
        // 更新目标状态
        goal.setStatus("ACTIVE");
        goalService.updateById(goal);
        return Result.ok();
    }
```

- [ ] **Step 6: 更新 AIResultRequest.PhaseDTO**

`common/src/main/java/com/learningplanner/common/dto/AIResultRequest.java`:
```java
package com.learningplanner.common.dto;

import lombok.Data;
import java.util.List;

@Data
public class AIResultRequest {
    private Long goalId;
    private String goalType;
    private String difficulty;
    private String estimatedDuration;
    private List<String> requiredSkills;
    private List<PhaseDTO> phases;

    @Data
    public static class PhaseDTO {
        private String phaseName;
        private Integer phaseOrder;
        private String phaseDesc;
        private Integer estimatedDays;
        private List<TaskDTO> tasks;

        @Data
        public static class TaskDTO {
            private String taskName;
            private String taskDesc;
            private Integer priority;
            private Integer estimatedHours;
        }
    }
}
```

- [ ] **Step 7: 验证编译**

```bash
mvn compile -pl common,planner-service
```
Expected: BUILD SUCCESS

- [ ] **Step 8: 提交**

```bash
git add planner-service/src/main/java/com/learningplanner/planner/service/LearningPhaseService.java planner-service/src/main/java/com/learningplanner/planner/service/LearningTaskService.java planner-service/src/main/java/com/learningplanner/planner/repository/ planner-service/src/main/java/com/learningplanner/planner/controller/LearningGoalController.java common/src/main/java/com/learningplanner/common/dto/AIResultRequest.java
git commit -m "feat: add AI result receiver endpoint to save roadmap"
```

---

### Task 9: 清理旧文件 + 添加健康检查

**Files:**
- Delete: `src/main/java/com/learningplannerbackend/LearningPlannerBackendApplication.java`
- Delete: `src/main/resources/application.properties`
- Delete: `src/test/java/com/learningplannerbackend/LearningPlannerBackendApplicationTests.java`
- Create: `gateway/src/main/java/com/learningplanner/gateway/controller/HealthController.java`

- [ ] **Step 1: 删除旧模块的遗留文件**

```bash
rm src/main/java/com/learningplannerbackend/LearningPlannerBackendApplication.java
rm src/main/resources/application.properties
rm src/test/java/com/learningplannerbackend/LearningPlannerBackendApplicationTests.java
rmdir src/main/java/com/learningplannerbackend 2>/dev/null
rmdir src/test/java/com/learningplannerbackend 2>/dev/null
rmdir src/main 2>/dev/null
```

- [ ] **Step 2: 在 Gateway 添加健康检查端点**

`gateway/src/main/java/com/learningplanner/gateway/controller/HealthController.java`:
```java
package com.learningplanner.gateway.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/api/health")
    public Mono<Map<String, String>> health() {
        return Mono.just(Map.of("status", "UP", "service", "gateway"));
    }
}
```

- [ ] **Step 3: 提交**

```bash
git rm src/main/java/com/learningplannerbackend/LearningPlannerBackendApplication.java src/main/resources/application.properties src/test/java/com/learningplannerbackend/LearningPlannerBackendApplicationTests.java
git add gateway/src/main/java/com/learningplanner/gateway/controller/HealthController.java
git commit -m "chore: remove legacy module files, add health check endpoint"
```

---

### Task 10: Python AI Service - 项目初始化

**Files:**
- Create: `ai-planner-service/requirements.txt`
- Create: `ai-planner-service/main.py`
- Create: `ai-planner-service/app/__init__.py`
- Create: `ai-planner-service/app/models/__init__.py`
- Create: `ai-planner-service/app/models/schemas.py`
- Create: `ai-planner-service/app/mq/__init__.py`
- Create: `ai-planner-service/app/mq/consumer.py`
- Create: `ai-planner-service/app/mq/producer.py`
- Create: `ai-planner-service/app/agent/__init__.py`
- Create: `ai-planner-service/app/agent/goal_analyzer.py`
- Create: `ai-planner-service/app/agent/roadmap_generator.py`
- Create: `ai-planner-service/app/agent/task_splitter.py`
- Create: `ai-planner-service/app/agent/workflow.py`
- Create: `ai-planner-service/app/prompts/__init__.py`
- Create: `ai-planner-service/app/prompts/templates.py`
- Create: `ai-planner-service/.env.example`

- [ ] **Step 1: 创建 requirements.txt**

`ai-planner-service/requirements.txt`:
```
fastapi==0.115.0
uvicorn==0.30.6
pydantic==2.9.2
langgraph==0.2.37
langchain==0.3.7
langchain-openai==0.2.6
pika==1.3.2
python-dotenv==1.0.1
```

- [ ] **Step 2: 创建 .env.example**

`ai-planner-service/.env.example`:
```
OPENAI_API_KEY=your-api-key-here
OPENAI_BASE_URL=https://api.openai.com/v1
LLM_MODEL=gpt-4o-mini
RABBITMQ_HOST=192.168.171.135
RABBITMQ_PORT=5672
RABBITMQ_USER=guest
RABBITMQ_PASS=guest
REDIS_HOST=192.168.171.135
REDIS_PORT=6379
REDIS_PASSWORD=redis123
```

- [ ] **Step 3: 创建数据模型**

`ai-planner-service/app/models/schemas.py`:
```python
from pydantic import BaseModel, Field
from typing import Optional


class GoalAnalysisRequest(BaseModel):
    goal_id: int
    user_id: int
    goal_name: str
    goal_desc: str = ""


class GoalAnalysis(BaseModel):
    goal_type: str
    difficulty: str  # beginner | intermediate | advanced
    estimated_duration: str
    required_skills: list[str]


class TaskItem(BaseModel):
    task_name: str
    task_desc: str
    priority: int = 1
    estimated_hours: int = 1


class PhaseItem(BaseModel):
    phase_name: str
    phase_order: int
    phase_desc: str
    estimated_days: int
    tasks: list[TaskItem]


class RoadmapResult(BaseModel):
    goal_id: int
    goal_type: str
    difficulty: str
    estimated_duration: str
    required_skills: list[str]
    phases: list[PhaseItem]
```

- [ ] **Step 4: 创建 main.py**

`ai-planner-service/main.py`:
```python
import os
import logging
from dotenv import load_dotenv

load_dotenv()

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)
logger = logging.getLogger("ai-planner")

from app.mq.consumer import start_consumer

if __name__ == "__main__":
    logger.info("Starting AI Planner Service...")
    start_consumer()
```

- [ ] **Step 5: 提交**

```bash
git add ai-planner-service/
git commit -m "feat: initialize Python AI service project structure"
```

---

### Task 11: Python AI Service - Prompt 模板

**Files:**
- Modify: `ai-planner-service/app/prompts/templates.py`

- [ ] **Step 1: 编写 Prompt 模板**

`ai-planner-service/app/prompts/templates.py`:
```python
GOAL_ANALYZER_PROMPT = """你是一个学习规划专家。分析用户的学习目标并给出评估。

用户目标: {goal_name}
目标描述: {goal_desc}

请以 JSON 格式返回分析结果，包含以下字段:
- goal_type: 目标类型 (如 frontend, backend, ai, algorithm, english 等)
- difficulty: 难度 (beginner, intermediate, advanced)
- estimated_duration: 预估学习时间 (如 "3个月", "6个月", "1年")
- required_skills: 需要掌握的技能列表

仅返回 JSON，不要包含其他内容。"""

ROADMAP_GENERATOR_PROMPT = """你是一个学习路线规划专家。根据以下目标分析结果，生成结构化的学习阶段。

目标: {goal_name}
目标类型: {goal_type}
难度: {difficulty}
预估时间: {estimated_duration}
所需技能: {required_skills}

请以 JSON 格式返回学习阶段列表。每个阶段包含:
- phase_name: 阶段名称
- phase_order: 阶段序号 (从1开始)
- phase_desc: 阶段描述 (100字以内)
- estimated_days: 预估天数

仅返回 JSON 数组，不要包含其他内容。
示例格式: [{{"phase_name": "Java基础", "phase_order": 1, "phase_desc": "...", "estimated_days": 30}}]"""

TASK_SPLITTER_PROMPT = """你是一个学习任务拆解专家。为以下学习阶段拆解具体任务。

学习阶段: {phase_name}
阶段描述: {phase_desc}
预估天数: {estimated_days}

请以 JSON 格式返回任务列表。每个任务包含:
- task_name: 任务名称
- task_desc: 任务描述 (50字以内)
- priority: 优先级 (1=高, 2=中, 3=低)
- estimated_hours: 预估小时数

仅返回 JSON 数组，不要包含其他内容。
示例格式: [{{"task_name": "环境搭建", "task_desc": "安装JDK和IDE", "priority": 1, "estimated_hours": 2}}]"""
```

- [ ] **Step 2: 提交**

```bash
git add ai-planner-service/app/prompts/
git commit -m "feat: add LangGraph agent prompt templates"
```

---

### Task 12: Python AI Service - LangGraph Agent 实现

**Files:**
- Modify: `ai-planner-service/app/agent/goal_analyzer.py`
- Modify: `ai-planner-service/app/agent/roadmap_generator.py`
- Modify: `ai-planner-service/app/agent/task_splitter.py`
- Modify: `ai-planner-service/app/agent/workflow.py`

- [ ] **Step 1: 实现 Goal Analyzer Agent**

`ai-planner-service/app/agent/goal_analyzer.py`:
```python
import json
import logging
from langchain_openai import ChatOpenAI
from langchain_core.messages import HumanMessage
from app.prompts.templates import GOAL_ANALYZER_PROMPT

logger = logging.getLogger(__name__)


def analyze_goal(state: dict) -> dict:
    """分析用户目标，返回类型、难度、预估时间、所需技能"""
    llm = ChatOpenAI(
        model=state.get("model", "gpt-4o-mini"),
        temperature=0.3,
    )
    prompt = GOAL_ANALYZER_PROMPT.format(
        goal_name=state["goal_name"],
        goal_desc=state.get("goal_desc", ""),
    )
    response = llm.invoke([HumanMessage(content=prompt)])
    analysis = json.loads(response.content.strip().removeprefix("```json").removesuffix("```").strip())
    logger.info(f"Goal analysis complete: type={analysis.get('goal_type')}")
    return {"analysis": analysis}
```

- [ ] **Step 2: 实现 Roadmap Generator Agent**

`ai-planner-service/app/agent/roadmap_generator.py`:
```python
import json
import logging
from langchain_openai import ChatOpenAI
from langchain_core.messages import HumanMessage
from app.prompts.templates import ROADMAP_GENERATOR_PROMPT

logger = logging.getLogger(__name__)


def generate_phases(state: dict) -> dict:
    """根据目标分析结果，生成学习阶段"""
    llm = ChatOpenAI(
        model=state.get("model", "gpt-4o-mini"),
        temperature=0.3,
    )
    analysis = state["analysis"]
    prompt = ROADMAP_GENERATOR_PROMPT.format(
        goal_name=state["goal_name"],
        goal_type=analysis.get("goal_type", ""),
        difficulty=analysis.get("difficulty", ""),
        estimated_duration=analysis.get("estimated_duration", ""),
        required_skills=", ".join(analysis.get("required_skills", [])),
    )
    response = llm.invoke([HumanMessage(content=prompt)])
    phases = json.loads(response.content.strip().removeprefix("```json").removesuffix("```").strip())
    logger.info(f"Phases generated: {len(phases)} phases")
    return {"phases": phases}
```

- [ ] **Step 3: 实现 Task Splitter Agent**

`ai-planner-service/app/agent/task_splitter.py`:
```python
import json
import logging
from langchain_openai import ChatOpenAI
from langchain_core.messages import HumanMessage
from app.prompts.templates import TASK_SPLITTER_PROMPT

logger = logging.getLogger(__name__)


def split_tasks(state: dict) -> dict:
    """为每个阶段拆分学习任务"""
    llm = ChatOpenAI(
        model=state.get("model", "gpt-4o-mini"),
        temperature=0.3,
    )
    phases = state["phases"]
    result_phases = []
    for phase in phases:
        prompt = TASK_SPLITTER_PROMPT.format(
            phase_name=phase["phase_name"],
            phase_desc=phase.get("phase_desc", ""),
            estimated_days=phase.get("estimated_days", 7),
        )
        response = llm.invoke([HumanMessage(content=prompt)])
        tasks = json.loads(response.content.strip().removeprefix("```json").removesuffix("```").strip())
        phase["tasks"] = tasks
        result_phases.append(phase)
        logger.info(f"Tasks generated for phase '{phase['phase_name']}': {len(tasks)} tasks")
    return {"phases": result_phases}
```

- [ ] **Step 4: 实现 LangGraph Workflow**

`ai-planner-service/app/agent/workflow.py`:
```python
import logging
from langgraph.graph import StateGraph, END
from typing import TypedDict, Optional
from app.agent.goal_analyzer import analyze_goal
from app.agent.roadmap_generator import generate_phases
from app.agent.task_splitter import split_tasks
from app.models.schemas import RoadmapResult

logger = logging.getLogger(__name__)


class PlannerState(TypedDict):
    goal_id: int
    user_id: int
    goal_name: str
    goal_desc: str
    model: str
    analysis: Optional[dict]
    phases: Optional[list[dict]]


def build_workflow() -> StateGraph:
    workflow = StateGraph(PlannerState)

    workflow.add_node("goal_analyzer", analyze_goal)
    workflow.add_node("roadmap_generator", generate_phases)
    workflow.add_node("task_splitter", split_tasks)

    workflow.set_entry_point("goal_analyzer")
    workflow.add_edge("goal_analyzer", "roadmap_generator")
    workflow.add_edge("roadmap_generator", "task_splitter")
    workflow.add_edge("task_splitter", END)

    return workflow.compile()


def run_planner_workflow(goal_id: int, user_id: int,
                         goal_name: str, goal_desc: str = "",
                         model: str = "gpt-4o-mini") -> RoadmapResult:
    """运行完整的规划工作流"""
    logger.info(f"Starting planner workflow for goal_id={goal_id}: {goal_name}")
    app = build_workflow()
    result = app.invoke({
        "goal_id": goal_id,
        "user_id": user_id,
        "goal_name": goal_name,
        "goal_desc": goal_desc,
        "model": model,
        "analysis": None,
        "phases": None,
    })
    analysis = result["analysis"]
    phases = result["phases"]
    logger.info(f"Workflow complete: {len(phases)} phases generated")
    return RoadmapResult(
        goal_id=goal_id,
        goal_type=analysis.get("goal_type", ""),
        difficulty=analysis.get("difficulty", "intermediate"),
        estimated_duration=analysis.get("estimated_duration", ""),
        required_skills=analysis.get("required_skills", []),
        phases=phases,
    )
```

- [ ] **Step 5: 提交**

```bash
git add ai-planner-service/app/agent/
git commit -m "feat: implement LangGraph agent workflow"
```

---

### Task 13: Python AI Service - MQ 集成

**Files:**
- Modify: `ai-planner-service/app/mq/consumer.py`
- Modify: `ai-planner-service/app/mq/producer.py`

- [ ] **Step 1: 实现 MQ Consumer**

`ai-planner-service/app/mq/consumer.py`:
```python
import json
import os
import logging
import threading
import pika
from app.agent.workflow import run_planner_workflow
from app.mq.producer import publish_result

logger = logging.getLogger(__name__)

RABBITMQ_HOST = os.getenv("RABBITMQ_HOST", "192.168.171.135")
RABBITMQ_PORT = int(os.getenv("RABBITMQ_PORT", "5672"))
RABBITMQ_USER = os.getenv("RABBITMQ_USER", "guest")
RABBITMQ_PASS = os.getenv("RABBITMQ_PASS", "guest")
AI_EXCHANGE = "ai.exchange"
GOAL_ANALYSIS_QUEUE = "goal.analysis.queue"


def on_message(ch, method, properties, body):
    """处理 Goal 分析请求"""
    try:
        msg = json.loads(body)
        logger.info(f"Received goal analysis request: {msg}")
        result = run_planner_workflow(
            goal_id=msg["goalId"],
            user_id=msg["userId"],
            goal_name=msg["goalName"],
            goal_desc=msg.get("goalDesc", ""),
        )
        publish_result(result.model_dump())
        ch.basic_ack(delivery_tag=method.delivery_tag)
        logger.info(f"Goal {msg['goalId']} processed successfully")
    except Exception as e:
        logger.error(f"Failed to process message: {e}", exc_info=True)
        ch.basic_nack(delivery_tag=method.delivery_tag, requeue=True)


def start_consumer():
    """启动 RabbitMQ 消费者"""
    credentials = pika.PlainCredentials(RABBITMQ_USER, RABBITMQ_PASS)
    connection = pika.BlockingConnection(
        pika.ConnectionParameters(
            host=RABBITMQ_HOST, port=RABBITMQ_PORT, credentials=credentials
        )
    )
    channel = connection.channel()
    channel.exchange_declare(exchange=AI_EXCHANGE, exchange_type="direct", durable=True)
    channel.queue_declare(queue=GOAL_ANALYSIS_QUEUE, durable=True)
    channel.queue_bind(
        queue=GOAL_ANALYSIS_QUEUE, exchange=AI_EXCHANGE, routing_key="goal.analysis"
    )
    channel.basic_qos(prefetch_count=1)
    channel.basic_consume(queue=GOAL_ANALYSIS_QUEUE, on_message_callback=on_message)
    logger.info(f"Listening for messages on queue: {GOAL_ANALYSIS_QUEUE}")
    channel.start_consuming()
```

- [ ] **Step 2: 实现 MQ Producer**

`ai-planner-service/app/mq/producer.py`:
```python
import json
import os
import logging
import pika

logger = logging.getLogger(__name__)

RABBITMQ_HOST = os.getenv("RABBITMQ_HOST", "192.168.171.135")
RABBITMQ_PORT = int(os.getenv("RABBITMQ_PORT", "5672"))
RABBITMQ_USER = os.getenv("RABBITMQ_USER", "guest")
RABBITMQ_PASS = os.getenv("RABBITMQ_PASS", "guest")
AI_EXCHANGE = "ai.exchange"
AI_RESULT_QUEUE = "ai.result.queue"


def publish_result(result: dict):
    """将 AI 生成结果发送回 Java"""
    credentials = pika.PlainCredentials(RABBITMQ_USER, RABBITMQ_PASS)
    connection = pika.BlockingConnection(
        pika.ConnectionParameters(
            host=RABBITMQ_HOST, port=RABBITMQ_PORT, credentials=credentials
        )
    )
    channel = connection.channel()
    channel.exchange_declare(exchange=AI_EXCHANGE, exchange_type="direct", durable=True)
    channel.queue_declare(queue=AI_RESULT_QUEUE, durable=True)
    channel.queue_bind(
        queue=AI_RESULT_QUEUE, exchange=AI_EXCHANGE, routing_key="ai.result"
    )
    channel.basic_publish(
        exchange=AI_EXCHANGE,
        routing_key="ai.result",
        body=json.dumps(result, ensure_ascii=False).encode("utf-8"),
        properties=pika.BasicProperties(
            delivery_mode=2,  # persistent
            content_type="application/json",
        ),
    )
    connection.close()
    logger.info(f"Published result for goal_id={result['goal_id']}")
```

- [ ] **Step 3: 提交**

```bash
git add ai-planner-service/app/mq/
git commit -m "feat: add RabbitMQ consumer/producer for Python AI service"
```

---

### Task 14: 需要调整 planner-service 移除重复的 MQ 配置

**Files:**
- Delete: `planner-service/src/main/java/com/learningplanner/planner/config/RabbitMQConfig.java` (已在 ai-gateway-service 定义)
- Modify: `planner-service/src/main/java/com/learningplanner/planner/mq/GoalAnalysisProducer.java` (引用路径调整)

- [ ] **Step 1: 删除 planner-service 中重复的 RabbitMQConfig，改为引用 ai-gateway-service 的配置**

planner-service 只需要 Producer，Queue/Exchange 定义由 ai-gateway-service 负责（因为它是消息的入口）。简化 Producer：

`planner-service/src/main/java/com/learningplanner/planner/mq/GoalAnalysisProducer.java`:
```java
package com.learningplanner.planner.mq;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class GoalAnalysisProducer {

    private static final String AI_EXCHANGE = "ai.exchange";

    private final RabbitTemplate rabbitTemplate;

    public GoalAnalysisProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendGoalAnalysis(Long goalId, Long userId, String goalName, String goalDesc) {
        rabbitTemplate.convertAndSend(AI_EXCHANGE, "goal.analysis",
                Map.of("goalId", goalId, "userId", userId,
                       "goalName", goalName, "goalDesc", goalDesc));
    }
}
```

删除 planner-service 的 RabbitMQConfig:
```bash
rm planner-service/src/main/java/com/learningplanner/planner/config/RabbitMQConfig.java
```

- [ ] **Step 2: 验证编译**

```bash
mvn compile -pl planner-service
```
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add planner-service/
git commit -m "refactor: remove duplicate MQ config, use ai-gateway queue definitions"
```

---

### Task 15: 确保 POM 中缺少的依赖版本被管理

**Files:**
- Modify: `pom.xml` (添加 amqp 依赖管理)

- [ ] **Step 1: 在父 POM 添加 Spring AMQP 依赖管理**

`pom.xml`, 在 `<dependencyManagement>` 内添加:
```xml
            <!-- Spring AMQP (RabbitMQ) -->
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-amqp</artifactId>
                <version>3.4.7</version>
            </dependency>
```

- [ ] **Step 2: 验证全量编译**

```bash
mvn compile
```
Expected: BUILD SUCCESS across all modules

- [ ] **Step 3: 提交**

```bash
git add pom.xml
git commit -m "fix: add spring-boot-starter-amqp to dependency management"
```

---

### Task 16: 端到端验证

- [ ] **Step 1: 启动基础设施**

确保 MySQL, Redis, RabbitMQ 在 192.168.171.135 上运行。

- [ ] **Step 2: 执行建表 SQL**

```bash
mysql -h 192.168.171.135 -u root -p123456 ai_platform < sql/V1__init_tables.sql
```
Expected: 5 张表创建成功

- [ ] **Step 3: 启动 Python 服务**

```bash
cd ai-planner-service
cp .env.example .env  # 填写真实 API Key
pip install -r requirements.txt
python main.py
```
Expected: "Listening for messages on queue: goal.analysis.queue"

- [ ] **Step 4: 启动 Java 服务**

按顺序启动:
```bash
# 每个服务一个终端
mvn spring-boot:run -pl gateway
mvn spring-boot:run -pl user-service
mvn spring-boot:run -pl planner-service
mvn spring-boot:run -pl ai-gateway-service
mvn spring-boot:run -pl task-service
```
Expected: 所有服务启动成功，注册到 Nacos

- [ ] **Step 5: 端到端测试**

```bash
# 1. 注册用户
curl -X POST http://localhost:8080/api/user/register \
  -H "Content-Type: application/json" \
  -d '{"username": "test", "password": "123456"}'

# 2. 登录获取 Token
curl -X POST http://localhost:8080/api/user/login \
  -H "Content-Type: application/json" \
  -d '{"username": "test", "password": "123456"}'
# 复制返回的 Token

# 3. 创建学习目标
curl -X POST http://localhost:8080/api/planner/goals \
  -H "Content-Type: application/json" \
  -H "Authorization: <token>" \
  -d '{"goalName": "学习Java后端", "goalDesc": "零基础想系统学习Java后端开发", "estimatedDuration": "6个月"}'

# 4. 查询结果 (等30-60秒AI响应后)
curl http://localhost:8080/api/planner/goals \
  -H "Authorization: <token>"
```
Expected:
- 注册返回 success
- 登录返回 Token
- 创建 Goal 返回 status=ANALYZING
- AI 处理后查询到完整的 Phase + Task

- [ ] **Step 6: 提交**

```bash
git add -A
git commit -m "feat: end-to-end integration verified"
```
