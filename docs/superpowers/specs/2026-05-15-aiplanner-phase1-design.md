# AIPlanner 第一阶段设计文档

## 1. 项目概述

**项目名称**：AIPlanner - AI 学习规划平台

**核心目标**：用户输入学习目标，系统通过 AI 自动生成学习路线（阶段划分、任务列表、推荐资源），并提供定时提醒和掌握度分析。

**第一阶段目标**：跑通核心闭环 —— 用户提交 Goal → Python AI 生成路线 → Java 保存并展示。

---

## 2. 系统架构

```
前端 (Vue3)
   ↓
Spring Cloud Gateway (Java)  ← JWT 鉴权、路由、限流
   ↓
┌─────────────────── Java 微服务集群 ──────────────────┐
│                                                       │
│  user-service       planner-service    task-service  │
│  (用户、登录)        (目标、路线管理)    (任务、进度)  │
│                                                       │
│  ai-gateway-service    reminder-service              │
│  (MQ调度、Python通信)   (Quartz定时提醒)              │
│                                                       │
│  websocket-service     common                        │
│  (Redis Pub/Sub推送)   (实体、DTO、配置)              │
│                                                       │
└───────────────────┬─────────────────────────────────┘
                    │ RabbitMQ
                    ↓
┌─────────── Python AI Service (FastAPI + LangGraph) ──┐
│                                                       │
│  Goal Analyzer Agent → Roadmap Generator Agent       │
│       → Task Splitter Agent → 结果汇总              │
│                                                       │
│  每步通过 Redis Pub/Sub → WebSocket 推送进度         │
└───────────────────────────────────────────────────────┘
                    ↓
              MySQL + Redis
```

---

## 3. 技术栈

### Java 侧
| 组件 | 选型 | 说明 |
|------|------|------|
| JDK | 17 | Spring Boot 3.4 最低要求 |
| 框架 | Spring Boot 3.4.7 | 基础框架 |
| 微服务 | Spring Cloud 2024.0.1 + Spring Cloud Alibaba 2023.0.3.2 | 服务注册/配置 |
| ORM | MyBatis Plus 3.5.10 | 持久层 |
| 认证 | Sa-Token 1.44.0 + JWT | 登录鉴权 |
| 网关 | Spring Cloud Gateway | 路由转发 |
| 消息队列 | RabbitMQ | 与 Python 异步通信 |
| 缓存 | Redis + Redisson | 缓存与分布式锁 |
| WebSocket | Spring WebSocket + Redis Pub/Sub | 实时推送 |
| 定时任务 | Quartz | 学习提醒 |

### Python 侧
| 组件 | 选型 | 说明 |
|------|------|------|
| Python | 3.12 | 运行时 |
| Web 框架 | FastAPI | HTTP 接口 |
| Agent 框架 | LangGraph | 多 Agent 工作流编排 |
| LLM | OpenAI API（可替换） | AI 推理 |
| MQ | pika | RabbitMQ 客户端 |
| 数据校验 | Pydantic | 结构化输出 |
| Prompt 管理 | Jinja2 | Prompt 模板 |

---

## 4. 数据库设计

### user
```
id (BIGINT PK), username (VARCHAR), password (VARCHAR),
avatar (VARCHAR), create_time (DATETIME)
```

### learning_goal
```
id (BIGINT PK), user_id (BIGINT FK), goal_name (VARCHAR),
goal_desc (TEXT), status (VARCHAR), estimated_duration (VARCHAR),
create_time (DATETIME)
```

### learning_phase
```
id (BIGINT PK), goal_id (BIGINT FK), phase_name (VARCHAR),
phase_order (INT), phase_desc (TEXT), mastery_score (INT),
status (VARCHAR), estimated_days (INT)
```

### learning_task
```
id (BIGINT PK), phase_id (BIGINT FK), task_name (VARCHAR),
task_desc (TEXT), status (VARCHAR), deadline (DATE),
progress (INT), priority (INT)
```

### reminder_record
```
id (BIGINT PK), task_id (BIGINT FK), user_id (BIGINT FK),
remind_time (DATETIME), remind_type (VARCHAR), status (VARCHAR)
```

---

## 5. 核心流程

```
[1] 用户 POST /api/planner/goals (创建学习目标)
       ↓
[2] planner-service: 保存 Goal (status=ANALYZING)
       ↓
[3] planner-service: 发送 MQ → goal.analysis.queue
       ↓
[4] Python AI 消费 → LangGraph Workflow 启动
       ├── GoalAnalyzerAgent: 分析目标类型/难度/预估时间
       ├── RoadmapGeneratorAgent: 生成学习阶段
       ├── TaskSplitterAgent: 每阶段拆解任务
       └── 汇总 JSON 结果 → 发送 ai.result.queue
       ↓
[5] ai-gateway-service 消费结果 → 调用 planner-service
       ↓
[6] planner-service: 保存 Phase + Task，更新 Goal (status=ACTIVE)
       ↓
[7] 返回学习路线给前端展示
```

### LangGraph Workflow 结构

```python
# 工作流状态
class PlannerState(TypedDict):
    goal: dict           # 原始目标信息
    analysis: dict       # Goal 分析结果
    phases: list[dict]   # 学习阶段
    tasks: list[dict]    # 学习任务
    progress: str        # 当前进度 (推给 WS)

# 工作流节点
start → goal_analyzer → roadmap_generator → task_splitter → result_publisher → end
```

### MQ 消息格式

**请求消息 (goal.analysis.queue)**：
```json
{
  "goalId": 1,
  "userId": 1,
  "goalName": "学习 Java 后端开发",
  "goalDesc": "零基础，想系统学习 Java 后端..."
}
```

**结果消息 (ai.result.queue)**：
```json
{
  "goalId": 1,
  "goalType": "backend",
  "difficulty": "medium",
  "estimatedDuration": "6 months",
  "requiredSkills": ["Java", "MySQL", "Spring"],
  "phases": [
    {
      "phaseName": "Java 基础",
      "phaseOrder": 1,
      "phaseDesc": "...",
      "estimatedDays": 30,
      "tasks": [
        {
          "taskName": "环境搭建",
          "taskDesc": "安装 JDK、IDE",
          "priority": 1,
          "estimatedHours": 2
        }
      ]
    }
  ]
}
```

---

## 6. Java 模块职责

### gateway
- 启动类：`GatewayApplication`
- 路由配置：`GatewayRoutesConfig` → 按路径转发到下游服务
- JWT 过滤：`JwtAuthFilter` → 校验 Token，放行 `/api/user/login`、`/api/user/register`

### user-service
- `POST /api/user/register` — 注册
- `POST /api/user/login` — 登录，返回 JWT Token
- `GET /api/user/info` — 获取当前用户信息

### planner-service
- `POST /api/planner/goals` — 创建学习目标（触发 AI 分析）
- `GET /api/planner/goals` — 分页查询用户的目标列表
- `GET /api/planner/goals/{id}` — 查看目标详情（含路线、阶段、任务）
- `DELETE /api/planner/goals/{id}` — 删除目标
- `POST /api/planner/goals/{id}/ai-result` — 接收 AI 结果回调(internal)
- `GET /api/planner/phases/{goalId}` — 查询阶段的阶段和任务

### task-service
- `PUT /api/tasks/{id}/status` — 更新任务状态
- `GET /api/tasks?userId=&status=` — 按用户和状态查询任务

### ai-gateway-service
- `AIProducer` — 发送消息到 `goal.analysis.queue`
- `AIResultConsumer` — 监听 `ai.result.queue`，调用 planner-service 保存结果
- `PythonAIClient` — Feign 接口，HTTP 调用 Python（备用）

### reminder-service (第一阶段只建表，接口不做)
- Quartz 配置就绪，第二阶段启用

### websocket-service (第一阶段只写配置，功能不做)
- WebSocket 配置就绪，第三阶段启用

---

## 7. 第一阶段交付清单

- [ ] MySQL 建表完成
- [ ] 用户注册/登录接口
- [ ] Gateway JWT 校验生效
- [ ] 用户创建 Goal 接口
- [ ] Java 发 MQ → Python 消费
- [ ] Python LangGraph Workflow 生成路线
- [ ] Python 发 MQ → Java 消费保存
- [ ] 前端可查询完整路线

---

## 8. 完整路线图

| 阶段 | 核心目标 | 关键交付 |
|------|---------|---------|
| 一 | 核心闭环 | Goal 提交 → Python AI 生成路线 → Java 保存展示 |
| 二 | 业务完善 | 阶段/任务 CRUD、状态流转、用户模块完整化 |
| 三 | WebSocket 流式 | 用户实时看到 AI 分析进度、Redis Pub/Sub 推送 |
| 四 | 定时提醒 | Quartz 每日提醒、任务超时、连续未学、学习报告 |
| 五 | 掌握度分析 | AI 评估掌握度、薄弱点分析、动态调整路线 |
| 六 | 高级特性 | RAG 知识检索、Memory 长期记忆、系统优化 |

---

## 9. 非功能需求

- AI 输出强制 JSON Schema 校验（Pydantic），防止不稳定
- AI 响应超时通过 MQ 异步化解，不阻塞 HTTP 请求
- 所有服务无状态，支持水平扩展
- 日志统一使用 SLF4J (Java) / logging (Python)
