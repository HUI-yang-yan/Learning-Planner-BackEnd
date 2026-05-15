# AIPlanner 项目完整实现路线（Java + Python）

# 一、项目目标

项目名称：AIPlanner

核心目标：

用户输入一个学习目标（Goal），系统通过 AI 自动生成：

- 学习路线
- 阶段划分
- 每阶段任务
- 推荐学习资源
- 掌握度分析
- 动态学习调整
- 定时提醒

最终形成：

```text
AI 学习规划 + AI 学习教练 + Agent 工作流
```

---

# 二、整体系统架构

```text
前端（Vue3）
      ↓
Spring Cloud Gateway
      ↓
Java 微服务集群
      ↓ MQ
Python AI Agent Service
      ↓
MySQL + Redis + VectorDB
```

---

# 三、Java 项目完整实现路线

# Java 项目定位

Java 负责：

- 系统架构
- 用户系统
- 任务系统
- 数据持久化
- 消息队列
- WebSocket
- 定时任务
- AI 调度
- 状态管理

Java 不负责 AI 推理。

---

# Java 第一阶段：项目基础搭建（必须优先完成）

阶段目标：

```text
完成整个系统的基础工程结构
```

---

## 1. 技术栈搭建

推荐技术：

```text
JDK 21
Spring Boot 3
Spring Cloud Alibaba
MySQL 8
Redis
RabbitMQ
MyBatis Plus
Sa-Token / JWT
OpenFeign
WebSocket
Quartz
Docker
```

---

## 2. 微服务划分

必须完成：

### gateway-service

职责：

- 网关转发
- JWT 校验
- 请求过滤
- 限流

---

### user-service

职责：

- 用户登录
- 注册
- 权限
- Token

---

### planner-service

职责：

- 学习目标管理
- 学习路线管理
- 学习阶段管理

---

### task-service

职责：

- 学习任务管理
- 任务状态
- 完成进度

---

### reminder-service

职责：

- 定时提醒
- 学习通知
- 超时检测

---

### ai-gateway-service

职责：

- 与 Python Agent 通信
- MQ 消息发送
- AI 结果接收

---

### websocket-service

职责：

- AI 流式输出
- 实时消息推送

---

# 第一阶段完成标准

你必须做到：

```text
用户可以登录
服务可以互相调用
数据库连接正常
Redis 正常
RabbitMQ 正常
```

---

# Java 第二阶段：核心业务模型设计

阶段目标：

```text
完成学习系统的数据模型
```

---

## 1. 用户模块

表：

```text
user
```

字段：

```text
id
username
password
avatar
create_time
```

---

## 2. 学习目标模块

表：

```text
learning_goal
```

字段：

```text
id
user_id
goal_name
goal_desc
status
estimated_duration
create_time
```

---

## 3. 学习阶段模块

表：

```text
learning_phase
```

字段：

```text
id
goal_id
phase_name
phase_order
mastery_score
status
```

---

## 4. 学习任务模块

表：

```text
learning_task
```

字段：

```text
id
phase_id
task_name
task_desc
status
deadline
progress
```

---

## 5. 提醒记录模块

表：

```text
reminder_record
```

字段：

```text
id
task_id
remind_time
status
```

---

# 第二阶段完成标准

必须完成：

```text
Goal CRUD
Phase CRUD
Task CRUD
任务状态更新
分页查询
```

---

# Java 第三阶段：AI 调度系统

阶段目标：

```text
Java 能与 Python AI 服务协同工作
```

---

## 1. MQ 架构设计

推荐：

```text
RabbitMQ
```

队列：

```text
goal.analysis.queue
roadmap.generate.queue
task.generate.queue
mastery.evaluate.queue
```

---

## 2. 用户创建 Goal 流程

流程：

```text
用户创建 Goal
    ↓
Java 保存 Goal
    ↓
发送 MQ 消息
    ↓
Python 消费
    ↓
AI 生成路线
    ↓
Java 接收结果
    ↓
保存路线
```

---

## 3. AI 结果回调

推荐两种方式：

### MQ 回调

Python：

```text
生成完成后发送结果队列
```

Java：

```text
监听结果队列
```

---

### HTTP 回调

Python 调用：

```text
POST /api/ai/result
```

推荐优先 MQ。

---

# 第三阶段完成标准

必须做到：

```text
创建 Goal 后
AI 自动生成学习路线
并保存数据库
```

---

# Java 第四阶段：WebSocket 实时输出

阶段目标：

```text
实现 AI 流式响应
```

---

## 实现内容

用户：

```text
创建学习目标
```

前端：

```text
实时看到 AI 输出：
正在分析目标...
正在生成路线...
正在拆分阶段...
```

---

## 推荐技术

```text
Spring WebSocket
STOMP
Redis Pub/Sub
```

---

## WebSocket 消息类型

```json
{
  "type": "AI_STREAM",
  "content": "正在生成学习路线"
}
```

---

# 第四阶段完成标准

必须做到：

```text
用户实时看到 AI 输出过程
```

---

# Java 第五阶段：定时任务系统

阶段目标：

```text
实现学习提醒与自动检测
```

---

## 推荐技术

```text
Quartz
```

或者：

```text
XXL-Job
```

---

## 实现功能

### 每日学习提醒

```text
每天 20:00 提醒学习
```

---

### 任务超时提醒

```text
任务超过 deadline 未完成
```

---

### 连续未学习提醒

```text
3 天未学习自动提醒
```

---

### 学习报告生成

```text
每周自动生成学习报告
```

---

# 第五阶段完成标准

必须做到：

```text
系统自动推送学习提醒
```

---

# Java 第六阶段：系统优化

阶段目标：

```text
提升系统稳定性与并发能力
```

---

## 1. Redis 缓存

缓存：

```text
学习路线
用户进度
热门 Goal
```

---

## 2. 限流

推荐：

```text
Sentinel
```

---

## 3. 分布式锁

推荐：

```text
Redisson
```

---

## 4. 日志链路追踪

推荐：

```text
SkyWalking
Sleuth
```



# 第六阶段完成标准

必须做到：

```text
系统可部署
支持高并发
```

---

# 四、Python 项目完整实现路线

# Python 项目定位

Python 负责：

```text
AI 推理
Agent 工作流
学习路线生成
掌握度分析
任务拆分
动态规划
知识图谱
```

Python 不负责业务 CRUD。

---

# Python 第一阶段：AI 服务基础搭建

阶段目标：

```text
完成 AI 服务基础结构
```

---

## 推荐技术栈

```text
Python 3.12
FastAPI
PydanticAI
LangGraph
Redis
RabbitMQ
SQLAlchemy
```

---

## 项目结构

```text
app
├── agent
├── workflow
├── tools
├── prompts
├── memory
├── models
├── mq
├── api
└── utils
```

---

## 基础接口

必须完成：

```text
POST /goal/analyze
POST /roadmap/generate
POST /task/generate
POST /mastery/evaluate
```

---

# 第一阶段完成标准

必须做到：

```text
Python 能接收 Goal
并返回 AI 分析结果
```

---

# Python 第二阶段：Goal 分析 Agent

阶段目标：

```text
AI 理解用户学习目标
```

---

## 输入

```text
我想学习 Java 后端
```

---

## 输出

```json
{
  "goal_type": "backend",
  "difficulty": "medium",
  "estimated_duration": "6 months",
  "required_skills": []
}
```

---

## 核心能力

### Goal 分类

例如：

```text
前端
后端
AI
算法
英语
```

---

### 难度分析

例如：

```text
beginner
intermediate
advanced
```

---

### 时间评估

例如：

```text
3个月
6个月
1年
```

---

# 第二阶段完成标准

必须做到：

```text
AI 能正确分析 Goal
```

---

# Python 第三阶段：学习路线生成 Agent

阶段目标：

```text
AI 自动生成学习路线
```

---

## 示例输出

```json
[
  {
    "phase": "Java基础"
  },
  {
    "phase": "数据库"
  },
  {
    "phase": "SpringBoot"
  }
]
```

---

## 必须实现

### 阶段划分

### 阶段顺序

### 阶段描述

### 推荐学习时间

---

# 第三阶段完成标准

必须做到：

```text
AI 自动生成结构化学习路线
```

---

# Python 第四阶段：任务拆解 Agent

阶段目标：

```text
AI 自动拆分学习任务
```

---

## 示例

阶段：

```text
SpringMVC
```

任务：

```text
学习 DispatcherServlet
学习参数绑定
完成 CRUD 项目
```

---

## 输出结构

```json
{
  "tasks": []
}
```

---

# 第四阶段完成标准

必须做到：

```text
每个阶段自动生成学习任务
```

---

# Python 第五阶段：掌握度分析 Agent

阶段目标：

```text
AI 评估用户学习程度
```

---

## 输入

```text
完成任务率
学习时长
测试成绩
```

---

## 输出

```json
{
  "mastery": 72,
  "weakness": ["Redis"]
}
```

---

## 必须实现

### 掌握度评分

### 薄弱点分析

### 学习建议

---

# 第五阶段完成标准

必须做到：

```text
AI 能动态分析用户掌握度
```

---

# Python 第六阶段：动态学习路线调整

阶段目标：

```text
AI 根据用户情况动态调整计划
```

---

## 示例

用户：

```text
Redis 掌握较差
```

AI：

```text
增加 Redis 基础任务
延长学习周期
推荐额外资源
```

---

## 核心能力

### 动态重规划

### 难度调整

### 任务补充

---

# 第六阶段完成标准

必须做到：

```text
学习路线可动态调整
```

---

# Python 第七阶段：Memory + RAG

阶段目标：

```text
让 AI 拥有长期学习记忆
```

---

## Memory 系统

记录：

```text
用户薄弱点
学习习惯
完成率
常见错误
```

---

## RAG 系统

推荐：

```text
Milvus
Qdrant
PgVector
```

---

## 实现功能

### 学习资源检索

### 技术文档问答

### 历史学习记忆

---

# 第七阶段完成标准

必须做到：

```text
AI 能记住用户学习情况
```

---

# 五、最终项目高级形态

最终项目会变成：

```text
AI 学习规划平台
+ Agent 工作流
+ 学习教练
+ 知识图谱
+ 动态路线调整
+ 长期记忆
```

---

# 六、项目开发优先级（非常重要）

# 第一优先级（必须先做）

```text
用户输入 Goal
AI 生成路线
Java 保存路线
前端展示
```

---

# 第二优先级

```text
阶段
任务
状态
提醒
```

---

# 第三优先级

```text
WebSocket
流式输出
```

---

# 第四优先级

```text
掌握度分析
动态路线调整
```

---

# 第五优先级

```text
RAG
Memory
知识图谱
Multi-Agent
```

---

# 七、项目核心难点（必须重点解决）

# 1. AI 输出不稳定

必须：

```text
强制 JSON 输出
```

推荐：

```text
Pydantic
response_format
JSON Schema
```

---

# 2. AI 响应时间过长

必须：

```text
MQ 异步化
WebSocket 流式输出
```

---

# 3. 学习路线质量不稳定

必须：

```text
Prompt 模板化
Few-shot
知识库增强
```

---

# 4. 长期上下文记忆

必须：

```text
Redis + Vector DB
```

---

# 八、最终推荐学习顺序

# Java 学习顺序

```text
SpringBoot
MyBatisPlus
Redis
RabbitMQ
SpringCloud
WebSocket
Quartz
Docker
```

---

# Python 学习顺序

```text
FastAPI
Pydantic
LangGraph
Agent Workflow
Redis
RAG
VectorDB
```

---

# 九、最终目标

最终形成：

```text
企业级 AI Planner 平台
```

项目亮点：

```text
AI Workflow
Agent 架构
MQ 异步解耦
实时流式输出
动态学习路线
掌握度分析
长期记忆
RAG
知识图谱
```

这个项目已经属于：

```text
真正的 AI 应用系统
```
而不是简单 AI Demo。

