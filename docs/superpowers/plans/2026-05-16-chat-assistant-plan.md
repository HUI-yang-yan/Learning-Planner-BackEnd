# 聊天助手功能 实现计划

> **给执行者:** 推荐使用 superpowers:subagent-driven-development 或 superpowers:executing-plans 按任务逐个实现。步骤使用 checkbox (`- [ ]`) 语法跟踪。

**目标:** 在 ai-planner-service 上扩展一个支持 SSE 流式响应、RAG 知识检索、Tool 调用的对话学习助手

**架构:** 扩展现有 ai-planner-service，新增 `app/chat/` 模块（agent、tools、rag、conversation），Java 侧新增 chat_message 持久化与历史查询

**技术栈:** Python (FastAPI + LangGraph + ChromaDB)、Java (Spring Boot + MyBatis-Plus)、RabbitMQ、Redis、SSE

---

## Phase 1: 数据库与 Java 基础设施

### 任务 1: 创建 chat_message 表

**文件:**
- 创建: `sql/V3__add_chat_message.sql`

- [ ] **Step 1: 编写建表 SQL**

```sql
CREATE TABLE IF NOT EXISTS `chat_message` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `conversation_id` VARCHAR(36) NOT NULL,
    `role` VARCHAR(10) NOT NULL COMMENT 'user / assistant',
    `content` TEXT NOT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_conv_page` (`conversation_id`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [ ] **Step 2: 提交**

```bash
git add sql/V3__add_chat_message.sql
git commit -m "feat: add chat_message table migration"
```

---

### 任务 2: 创建 ChatMessage 实体

**文件:**
- 创建: `common/src/main/java/com/learningplanner/common/entity/ChatMessage.java`

- [ ] **Step 1: 编写实体**

```java
package com.learningplanner.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("chat_message")
public class ChatMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String conversationId;
    private String role;
    private String content;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
```

- [ ] **Step 2: 编译验证**

```bash
cd common && mvn compile -q
```

- [ ] **Step 3: 提交**

```bash
git add common/src/main/java/com/learningplanner/common/entity/ChatMessage.java
git commit -m "feat: add ChatMessage entity"
```

---

### 任务 3: 创建 ChatMessageMapper

**文件:**
- 创建: `planner-service/src/main/java/com/learningplanner/planner/repository/ChatMessageMapper.java`

- [ ] **Step 1: 编写 Mapper**

```java
package com.learningplanner.planner.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.learningplanner.common.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
}
```

- [ ] **Step 2: 提交**

```bash
git add planner-service/src/main/java/com/learningplanner/planner/repository/ChatMessageMapper.java
git commit -m "feat: add ChatMessageMapper"
```

---

### 任务 4: RabbitMQ 配置新增 chat.message 队列

**文件:**
- 修改: `planner-service/src/main/java/com/learningplanner/planner/config/RabbitMQConfig.java`

- [ ] **Step 1: 新增队列、绑定**

在 `RabbitMQConfig.java` 中新增以下内容（常量区追加，Bean 区追加）:

在类常量区添加:
```java
public static final String CHAT_MESSAGE_QUEUE = "chat.message.queue";
```

在 Bean 定义区添加:
```java
@Bean
public Queue chatMessageQueue() { return QueueBuilder.durable(CHAT_MESSAGE_QUEUE).build(); }

@Bean
public Binding chatMessageBinding() {
    return BindingBuilder.bind(chatMessageQueue()).to(aiExchange()).with("chat.message");
}
```

- [ ] **Step 2: 提交**

```bash
git add planner-service/src/main/java/com/learningplanner/planner/config/RabbitMQConfig.java
git commit -m "feat: add chat.message queue and binding"
```

---

### 任务 5: 创建 ChatMessageConsumer

**文件:**
- 创建: `planner-service/src/main/java/com/learningplanner/planner/mq/ChatMessageConsumer.java`

- [ ] **Step 1: 编写 Consumer**

```java
package com.learningplanner.planner.mq;

import com.learningplanner.common.entity.ChatMessage;
import com.learningplanner.planner.config.RabbitMQConfig;
import com.learningplanner.planner.repository.ChatMessageMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class ChatMessageConsumer {

    private final ChatMessageMapper chatMessageMapper;

    public ChatMessageConsumer(ChatMessageMapper chatMessageMapper) {
        this.chatMessageMapper = chatMessageMapper;
    }

    @SuppressWarnings("unchecked")
    @RabbitListener(queues = RabbitMQConfig.CHAT_MESSAGE_QUEUE)
    public void handleChatMessage(Map<String, Object> msg) {
        log.info("[Chat] Saving message: convId={}, role={}",
                msg.get("conversationId"), msg.get("role"));
        try {
            ChatMessage entity = new ChatMessage();
            entity.setUserId(Long.valueOf(msg.get("userId").toString()));
            entity.setConversationId((String) msg.get("conversationId"));
            entity.setRole((String) msg.get("role"));
            entity.setContent((String) msg.get("content"));
            chatMessageMapper.insert(entity);
        } catch (Exception e) {
            log.error("[Chat] Failed to save message", e);
            throw e;
        }
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
cd planner-service && mvn compile -q
```

- [ ] **Step 3: 提交**

```bash
git add planner-service/src/main/java/com/learningplanner/planner/mq/ChatMessageConsumer.java
git commit -m "feat: add ChatMessageConsumer for async message persistence"
```

---

### 任务 6: 创建 ChatHistoryController

**文件:**
- 创建: `planner-service/src/main/java/com/learningplanner/planner/controller/ChatHistoryController.java`

- [ ] **Step 1: 编写 Controller**

```java
package com.learningplanner.planner.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.learningplanner.common.dto.Result;
import com.learningplanner.common.entity.ChatMessage;
import com.learningplanner.planner.repository.ChatMessageMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Tag(name = "聊天历史", description = "分页查询对话历史记录")
@RestController
@RequestMapping("/api/planner/chat")
public class ChatHistoryController {

    private final ChatMessageMapper chatMessageMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    public ChatHistoryController(ChatMessageMapper chatMessageMapper,
                                  RedisTemplate<String, Object> redisTemplate) {
        this.chatMessageMapper = chatMessageMapper;
        this.redisTemplate = redisTemplate;
    }

    @Operation(summary = "分页查询对话历史", description = "按 conversationId 分页拉取消息，每页 20 条")
    @GetMapping("/history")
    public Result<Map<String, Object>> getHistory(
            @Parameter(description = "会话 ID") @RequestParam String conversationId,
            @Parameter(description = "偏移量") @RequestParam(defaultValue = "0") int offset) {
        Long userId = StpUtil.getLoginIdAsLong();
        String cacheKey = "chat:history:" + userId + ":" + conversationId + ":" + (offset / 20);

        // 尝试从 Redis 缓存读取
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof List) {
            @SuppressWarnings("unchecked")
            List<ChatMessage> list = (List<ChatMessage>) cached;
            return Result.ok(Map.of("items", list, "hasMore", list.size() == 20, "nextOffset", offset + list.size()));
        }

        // 缓存未命中，查 MySQL
        List<ChatMessage> messages = chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getConversationId, conversationId)
                        .orderByAsc(ChatMessage::getId)
                        .last("LIMIT 20 OFFSET " + offset));

        // 写入 Redis 缓存（10 分钟过期）
        redisTemplate.opsForValue().set(cacheKey, messages, 10, TimeUnit.MINUTES);

        return Result.ok(Map.of(
                "items", messages,
                "hasMore", messages.size() == 20,
                "nextOffset", offset + messages.size()));
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
cd planner-service && mvn compile -q
```

- [ ] **Step 3: 提交**

```bash
git add planner-service/src/main/java/com/learningplanner/planner/controller/ChatHistoryController.java
git commit -m "feat: add ChatHistoryController with Redis-cached pagination"
```

---

## Phase 2: Python 基础设施

### 任务 7: 更新 schemas.py

**文件:**
- 修改: `ai-planner-service/app/models/schemas.py`

- [ ] **Step 1: 追加 ChatRequest 和 ChatEvent 模型**

在文件末尾追加:

```python
class ChatRequest(BaseModel):
    message: str
    conversation_id: str | None = None


class ToolCallEvent(BaseModel):
    type: str = "tool_call"
    tool: str
    args: dict


class TokenEvent(BaseModel):
    type: str = "token"
    content: str


class DoneEvent(BaseModel):
    type: str = "done"
    conversation_id: str
```

- [ ] **Step 2: 提交**

```bash
git add ai-planner-service/app/models/schemas.py
git commit -m "feat: add ChatRequest and chat event schemas"
```

---

### 任务 8: 更新 Prompt 模板

**文件:**
- 修改: `ai-planner-service/app/prompts/templates.py`

- [ ] **Step 1: 追加 CHAT_SYSTEM_PROMPT**

在文件末尾追加:

```python
CHAT_SYSTEM_PROMPT = """你是一个专业的学习助手，负责帮助用户解决学习相关的问题。

你的能力包括：
1. 回答学习困惑 - 利用知识库检索相关学习资料，解释概念、对比技术、提供学习建议
2. 查询学习数据 - 查看用户的学习目标、阶段进度、掌握度评估结果
3. 创建学习目标 - 帮助用户创建新的学习目标并自动生成学习路线

回答原则：
- 使用中文回复，语气亲切专业
- 回答前先检索相关知识库文档，确保内容准确
- 如果用户的问题涉及他们的学习数据，主动调用工具查询
- 给出具体的、可操作的建议，而非泛泛而谈
- 如果不确定，如实告知并建议用户从哪些渠道获取信息

当前用户信息：
{user_context}
"""
```

- [ ] **Step 2: 提交**

```bash
git add ai-planner-service/app/prompts/templates.py
git commit -m "feat: add CHAT_SYSTEM_PROMPT for chat assistant"
```

---

### 任务 9: 更新依赖和配置

**文件:**
- 修改: `ai-planner-service/requirements.txt`
- 修改: `ai-planner-service/.env.example`

- [ ] **Step 1: 更新 requirements.txt**

在 `requirements.txt` 末尾追加:

```
chromadb==0.5.20
httpx==0.27.2
```

- [ ] **Step 2: 更新 .env.example**

在 `.env.example` 末尾追加:

```
CHROMADB_HOST=192.168.171.135
CHROMADB_PORT=8001
EMBEDDING_MODEL=text-embedding-v3
CHAT_MAX_HISTORY=20
CHAT_MODEL=deepseek-v4-pro
```

- [ ] **Step 3: 安装新依赖**

```bash
cd ai-planner-service && pip install chromadb==0.5.20 httpx==0.27.2 -q
```

- [ ] **Step 4: 提交**

```bash
git add ai-planner-service/requirements.txt ai-planner-service/.env.example
git commit -m "feat: add chromadb and httpx dependencies, env config for chat"
```

---

## Phase 3: Python 聊天模块

### 任务 10: 实现 RAG 检索器

**文件:**
- 创建: `ai-planner-service/app/chat/__init__.py`
- 创建: `ai-planner-service/app/chat/rag.py`

- [ ] **Step 1: 创建 __init__.py**

```bash
touch ai-planner-service/app/chat/__init__.py
```

- [ ] **Step 2: 编写 rag.py**

```python
import os
import logging
import chromadb
from chromadb.config import Settings
from langchain_openai import OpenAIEmbeddings

logger = logging.getLogger(__name__)

CHROMADB_HOST = os.getenv("CHROMADB_HOST", "192.168.171.135")
CHROMADB_PORT = os.getenv("CHROMADB_PORT", "8001")
EMBEDDING_MODEL = os.getenv("EMBEDDING_MODEL", "text-embedding-v3")

_embeddings = None
_chroma_client = None


def _get_embeddings():
    global _embeddings
    if _embeddings is None:
        _embeddings = OpenAIEmbeddings(
            model=EMBEDDING_MODEL,
            api_key=os.getenv("OPENAI_API_KEY"),
            base_url=os.getenv("OPENAI_BASE_URL"),
        )
    return _embeddings


def _get_chroma_client():
    global _chroma_client
    if _chroma_client is None:
        _chroma_client = chromadb.HttpClient(
            host=CHROMADB_HOST,
            port=CHROMADB_PORT,
            settings=Settings(anonymized_telemetry=False),
        )
    return _chroma_client


def retrieve_context(query: str, subject: str = None,
                     difficulty: str = None, top_k: int = 5) -> list[dict]:
    """从 ChromaDB 检索相关知识文档"""
    client = _get_chroma_client()
    embeddings = _get_embeddings()

    collection_names = [subject] if subject else _list_collections(client)
    all_docs = []

    for coll_name in collection_names:
        try:
            collection = client.get_collection(coll_name)
        except Exception:
            continue

        where_filter = {}
        if difficulty:
            where_filter["difficulty"] = difficulty

        query_embedding = embeddings.embed_query(query)
        results = collection.query(
            query_embeddings=[query_embedding],
            n_results=top_k,
            where=where_filter if where_filter else None,
        )

        if results and results.get("metadatas") and results["metadatas"][0]:
            for i, metadata in enumerate(results["metadatas"][0]):
                all_docs.append({
                    "content": results["documents"][0][i] if results.get("documents") else "",
                    "metadata": metadata,
                })
        if len(all_docs) >= top_k:
            break

    # 截断到 top_k
    return all_docs[:top_k]


def _list_collections(client) -> list[str]:
    """获取所有 collection 名称"""
    try:
        return [c.name for c in client.list_collections()]
    except Exception:
        return []
```

- [ ] **Step 3: 编写测试**

创建 `ai-planner-service/tests/test_rag.py`:

```python
from unittest.mock import patch, MagicMock
from app.chat.rag import retrieve_context


@patch("app.chat.rag._get_chroma_client")
@patch("app.chat.rag._get_embeddings")
def test_retrieve_context_returns_docs(mock_emb, mock_chroma):
    mock_collection = MagicMock()
    mock_collection.query.return_value = {
        "documents": [["Python 是一门解释型语言..."]],
        "metadatas": [[{"subject": "Python", "difficulty": "beginner"}]],
    }
    mock_client = MagicMock()
    mock_client.get_collection.return_value = mock_collection
    mock_chroma.return_value = mock_client
    mock_emb.return_value.embed_query.return_value = [0.1] * 1024

    result = retrieve_context("什么是 Python")
    assert len(result) == 1
    assert "Python" in result[0]["content"]
```

- [ ] **Step 4: 运行测试**

```bash
cd ai-planner-service && python -m pytest tests/test_rag.py -v
```

- [ ] **Step 5: 提交**

```bash
git add ai-planner-service/app/chat/__init__.py ai-planner-service/app/chat/rag.py ai-planner-service/tests/test_rag.py
git commit -m "feat: add ChromaDB RAG retriever with metadata filtering"
```

---

### 任务 11: 实现 Tool 定义

**文件:**
- 创建: `ai-planner-service/app/chat/tools.py`

- [ ] **Step 1: 编写 tools.py**

```python
import os
import logging
import httpx
from langchain_core.tools import tool

logger = logging.getLogger(__name__)

PLANNER_SERVICE_URL = os.getenv("PLANNER_SERVICE_URL", "http://planner-service:8080")


def _get_headers(auth_header: str = None) -> dict:
    headers = {"Content-Type": "application/json"}
    if auth_header:
        headers["Authorization"] = auth_header
    return headers


@tool
async def list_user_goals(auth_header: str = None) -> str:
    """查询当前用户的所有学习目标。返回目标 ID、名称、状态（ANALYZING/ACTIVE/PAUSED/COMPLETED）。"""
    async with httpx.AsyncClient(timeout=10) as client:
        resp = await client.get(
            f"{PLANNER_SERVICE_URL}/api/planner/goals",
            headers=_get_headers(auth_header),
        )
        resp.raise_for_status()
        data = resp.json()
        if data.get("code") == 200:
            return str(data["data"])
        return f"查询失败: {data.get('msg', '未知错误')}"


@tool
async def get_goal_phases(goal_id: int, auth_header: str = None) -> str:
    """查询指定学习目标的阶段列表和进度。参数 goal_id 为目标 ID。"""
    async with httpx.AsyncClient(timeout=10) as client:
        resp = await client.get(
            f"{PLANNER_SERVICE_URL}/api/planner/phases/page",
            params={"goalId": goal_id, "page": 1, "size": 20},
            headers=_get_headers(auth_header),
        )
        resp.raise_for_status()
        data = resp.json()
        if data.get("code") == 200:
            return str(data["data"])
        return f"查询失败: {data.get('msg', '未知错误')}"


@tool
async def get_mastery_result(goal_id: int, auth_header: str = None) -> str:
    """查询指定目标最新的掌握度评估结果，包含评分、薄弱点和改进建议。参数 goal_id 为目标 ID。"""
    async with httpx.AsyncClient(timeout=10) as client:
        resp = await client.get(
            f"{PLANNER_SERVICE_URL}/api/planner/ai/mastery/result/{goal_id}",
            headers=_get_headers(auth_header),
        )
        resp.raise_for_status()
        data = resp.json()
        if data.get("code") == 200:
            return str(data["data"])
        return f"暂无评估结果"


@tool
async def create_learning_goal(goal_name: str, goal_desc: str = "",
                               estimated_duration: str = "", auth_header: str = None) -> str:
    """创建新的学习目标，系统将自动分析并生成学习路线。参数 goal_name 为目标名称（必填），
    goal_desc 为目标描述，estimated_duration 为预计学习时长如 '3个月'。"""
    async with httpx.AsyncClient(timeout=10) as client:
        resp = await client.post(
            f"{PLANNER_SERVICE_URL}/api/planner/goals",
            json={
                "goalName": goal_name,
                "goalDesc": goal_desc,
                "estimatedDuration": estimated_duration,
            },
            headers=_get_headers(auth_header),
        )
        resp.raise_for_status()
        data = resp.json()
        if data.get("code") == 200:
            return f"已创建学习目标「{goal_name}」，正在生成学习路线，稍后可在目标列表中查看。"
        return f"创建失败: {data.get('msg', '未知错误')}"
```

- [ ] **Step 2: 编写测试**

创建 `ai-planner-service/tests/test_tools.py`:

```python
import pytest
from unittest.mock import patch, AsyncMock
from app.chat.tools import list_user_goals, create_learning_goal


@pytest.mark.asyncio
@patch("app.chat.tools.httpx.AsyncClient")
async def test_list_user_goals_success(mock_client):
    mock_instance = AsyncMock()
    mock_resp = AsyncMock()
    mock_resp.json.return_value = {"code": 200, "data": {"records": []}}
    mock_resp.raise_for_status = MagicMock()
    mock_instance.get.return_value = mock_resp
    mock_client.return_value.__aenter__.return_value = mock_instance

    result = await list_user_goals.ainvoke({})
    assert "records" in result


@pytest.mark.asyncio
@patch("app.chat.tools.httpx.AsyncClient")
async def test_create_learning_goal_success(mock_client):
    mock_instance = AsyncMock()
    mock_resp = AsyncMock()
    mock_resp.json.return_value = {"code": 200, "data": {}}
    mock_resp.raise_for_status = MagicMock()
    mock_instance.post.return_value = mock_resp
    mock_client.return_value.__aenter__.return_value = mock_instance

    result = await create_learning_goal.ainvoke({"goal_name": "学 Python"})
    assert "已创建" in result
```

- [ ] **Step 3: 运行测试**

```bash
cd ai-planner-service && python -m pytest tests/test_tools.py -v
```

- [ ] **Step 4: 提交**

```bash
git add ai-planner-service/app/chat/tools.py ai-planner-service/tests/test_tools.py
git commit -m "feat: add chat tools for goal query and creation"
```

---

### 任务 12: 实现对话历史管理

**文件:**
- 创建: `ai-planner-service/app/chat/conversation.py`

- [ ] **Step 1: 编写 conversation.py**

```python
import os
import json
import logging
import redis

logger = logging.getLogger(__name__)

REDIS_HOST = os.getenv("REDIS_HOST", "192.168.171.135")
REDIS_PORT = int(os.getenv("REDIS_PORT", "6379"))
REDIS_PASSWORD = os.getenv("REDIS_PASSWORD", "redis123")
CHAT_MAX_HISTORY = int(os.getenv("CHAT_MAX_HISTORY", "20"))


def _get_redis():
    return redis.Redis(
        host=REDIS_HOST, port=REDIS_PORT, password=REDIS_PASSWORD,
        decode_responses=True, socket_connect_timeout=3,
    )


def load_history(user_id: int, conversation_id: str) -> list[dict]:
    """从 Redis 加载最近 N 条对话历史"""
    try:
        r = _get_redis()
        key = f"chat:history:{user_id}:{conversation_id}:recent"
        data = r.lrange(key, 0, CHAT_MAX_HISTORY - 1)
        r.close()
        messages = [json.loads(item) for item in reversed(data)]
        return messages
    except Exception as e:
        logger.error(f"Failed to load chat history: {e}")
        return []


def save_message(user_id: int, conversation_id: str,
                 role: str, content: str):
    """保存一条消息到 Redis 历史"""
    try:
        r = _get_redis()
        key = f"chat:history:{user_id}:{conversation_id}:recent"
        msg = json.dumps({
            "role": role,
            "content": content,
        }, ensure_ascii=False)
        r.lpush(key, msg)
        r.ltrim(key, 0, CHAT_MAX_HISTORY - 1)
        r.expire(key, 3600)  # 1 小时过期
        r.close()
    except Exception as e:
        logger.error(f"Failed to save chat message: {e}")
```

- [ ] **Step 2: 提交**

```bash
git add ai-planner-service/app/chat/conversation.py
git commit -m "feat: add conversation history with Redis"
```

---

### 任务 13: 实现 LangGraph 聊天 Agent

**文件:**
- 创建: `ai-planner-service/app/chat/agent.py`

- [ ] **Step 1: 编写 agent.py**

```python
import os
import json
import logging
import uuid
from typing import AsyncIterator
from langchain_openai import ChatOpenAI
from langchain_core.messages import HumanMessage, SystemMessage, AIMessage, ToolMessage
from app.chat.rag import retrieve_context
from app.chat.tools import (
    list_user_goals, get_goal_phases, get_mastery_result, create_learning_goal,
)
from app.chat.conversation import load_history, save_message
from app.prompts.templates import CHAT_SYSTEM_PROMPT

logger = logging.getLogger(__name__)

CHAT_MODEL = os.getenv("CHAT_MODEL", "deepseek-v4-pro")

TOOLS = [list_user_goals, get_goal_phases, get_mastery_result, create_learning_goal]


def _build_system_prompt(user_id: int) -> str:
    """构建系统 prompt，包含用户上下文"""
    try:
        from app.memory.user_memory import get_user_summary
        summary = get_user_summary(user_id)
        user_context = json.dumps(summary, ensure_ascii=False)
    except Exception:
        user_context = "暂无历史学习数据"
    return CHAT_SYSTEM_PROMPT.format(user_context=user_context)


async def run_chat_agent(user_id: int, user_message: str,
                         conversation_id: str = None,
                         auth_header: str = None) -> AsyncIterator[dict]:
    """运行聊天 Agent，流式返回事件"""
    if not conversation_id:
        conversation_id = str(uuid.uuid4())

    llm = ChatOpenAI(model=CHAT_MODEL, temperature=0.7, streaming=True)
    llm_with_tools = llm.bind_tools(TOOLS)

    # 加载历史和 RAG
    history = load_history(user_id, conversation_id)
    rag_docs = retrieve_context(user_message)
    system_prompt = _build_system_prompt(user_id)

    # 拼装 RAG 上下文
    rag_text = ""
    if rag_docs:
        rag_text = "\n\n相关学习资料：\n"
        for doc in rag_docs:
            title = doc.get("metadata", {}).get("title", "资料")
            rag_text += f"- [{title}] {doc['content'][:200]}\n"

    # 构建消息列表
    messages = [SystemMessage(content=system_prompt + rag_text)]
    for h in history:
        if h["role"] == "user":
            messages.append(HumanMessage(content=h["content"]))
        else:
            messages.append(AIMessage(content=h["content"]))
    messages.append(HumanMessage(content=user_message))

    # 保存用户消息
    save_message(user_id, conversation_id, "user", user_message)

    full_response = ""

    try:
        # 第一轮：LLM 决策
        response = await llm_with_tools.ainvoke(messages)

        # 检查是否需要调用工具
        if response.tool_calls:
            for tool_call in response.tool_calls:
                tool_name = tool_call["name"]
                tool_args = tool_call["args"]
                yield {"type": "tool_call", "tool": tool_name, "args": tool_args}

                # 执行工具
                tool_func = {
                    "list_user_goals": list_user_goals,
                    "get_goal_phases": get_goal_phases,
                    "get_mastery_result": get_mastery_result,
                    "create_learning_goal": create_learning_goal,
                }.get(tool_name)

                if tool_func:
                    if tool_name == "list_user_goals":
                        args = {"auth_header": auth_header}
                    else:
                        args = {**tool_args, "auth_header": auth_header}
                    tool_result = await tool_func.ainvoke(args)

                    messages.append(response)
                    messages.append(ToolMessage(content=tool_result, tool_call_id=tool_call["id"]))

            # 第二轮：基于工具结果生成回复
            stream_llm = ChatOpenAI(model=CHAT_MODEL, temperature=0.7, streaming=True)
            async for chunk in stream_llm.astream(messages):
                if chunk.content:
                    full_response += chunk.content
                    yield {"type": "token", "content": chunk.content}
        else:
            # 无工具调用，流式返回
            if response.content:
                full_response = response.content
                yield {"type": "token", "content": response.content}

    except Exception as e:
        logger.error(f"Chat agent error: {e}", exc_info=True)
        yield {"type": "error", "content": f"处理出错: {str(e)}"}

    # 保存助手回复
    if full_response:
        save_message(user_id, conversation_id, "assistant", full_response)

    # 通知完成
    yield {"type": "done", "conversation_id": conversation_id}
```

- [ ] **Step 2: 提交**

```bash
git add ai-planner-service/app/chat/agent.py
git commit -m "feat: add LangGraph chat agent with tools and RAG"
```

---

### 任务 14: 创建 SSE 聊天端点

**文件:**
- 创建: `ai-planner-service/app/api/chat.py`

- [ ] **Step 1: 编写 chat.py**

```python
import json
import logging
from fastapi import APIRouter, Request
from fastapi.responses import StreamingResponse
from app.chat.agent import run_chat_agent
from app.models.schemas import ChatRequest

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/planner/ai", tags=["chat"])


@router.post("/chat")
async def chat(req: ChatRequest, request: Request):
    """聊天 SSE 端点"""

    # 从请求头提取认证信息
    auth_header = request.headers.get("Authorization")
    # 从 Sa-Token 或 JWT 中提取 user_id（此处简化：前端传 header）
    user_id_str = request.headers.get("X-User-Id", "0")
    user_id = int(user_id_str)

    async def event_stream():
        async for event in run_chat_agent(
            user_id=user_id,
            user_message=req.message,
            conversation_id=req.conversation_id,
            auth_header=auth_header,
        ):
            yield f"data: {json.dumps(event, ensure_ascii=False)}\n\n"

    return StreamingResponse(
        event_stream(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no",
        },
    )
```

- [ ] **Step 2: 注册路由到主应用**

修改 `ai-planner-service/app/api/routes.py`，在 `app` 创建后添加:

```python
from app.api.chat import router as chat_router
app.include_router(chat_router)
```

- [ ] **Step 3: 提交**

```bash
git add ai-planner-service/app/api/chat.py ai-planner-service/app/api/routes.py
git commit -m "feat: add SSE chat endpoint"
```

---

## Phase 4: 网关与集成

### 任务 15: 网关新增 AI 服务路由

**文件:**
- 修改: `gateway/src/main/java/com/learningplanner/gateway/config/GatewayRoutesConfig.java`

- [ ] **Step 1: 添加 AI 服务路由**

在 `GatewayRoutesConfig.java` 中，在 `planner-service` 路由**之前**添加:

```java
// AI Planner Chat Service（必须在 planner-service 之前，更具体的路由优先匹配）
.route("ai-planner-chat", r -> r
        .path("/api/planner/ai/chat")
        .uri("lb://ai-planner-service"))
```

同时需要确保 Nacos 中注册了 `ai-planner-service`。检查 Python 服务 Nacos 注册代码确认服务名。

- [ ] **Step 2: 验证认证过滤器放行**

检查 `gateway/src/main/java/com/learningplanner/gateway/filter/JwtAuthFilter.java`，确认 `/api/planner/ai/chat` 路径不会被拦截（或需要登录态）。如果需要登录，前端通过 gateway 时自动携带 token 即可。

- [ ] **Step 3: 编译验证**

```bash
cd gateway && mvn compile -q
```

- [ ] **Step 4: 提交**

```bash
git add gateway/src/main/java/com/learningplanner/gateway/config/GatewayRoutesConfig.java
git commit -m "feat: add gateway route for ai-planner-service chat"
```

---

### 任务 16: 知识导入脚本

**文件:**
- 创建: `scripts/ingest_knowledge.py`

- [ ] **Step 1: 编写导入脚本**

```python
#!/usr/bin/env python3
"""将 Markdown 知识文档批量导入 ChromaDB"""
import os
import sys
import argparse
import chromadb
from chromadb.config import Settings
from langchain_openai import OpenAIEmbeddings
from langchain_text_splitters import RecursiveCharacterTextSplitter

CHROMADB_HOST = os.getenv("CHROMADB_HOST", "192.168.171.135")
CHROMADB_PORT = os.getenv("CHROMADB_PORT", "8001")

# 通过环境变量加载 llm key/base_url
from dotenv import load_dotenv
load_dotenv()


def main():
    parser = argparse.ArgumentParser(description="导入知识文档到 ChromaDB")
    parser.add_argument("--subject", required=True, help="学科名，作为 collection 名称")
    parser.add_argument("--dir", required=True, help="Markdown 文档目录")
    parser.add_argument("--difficulty", default="intermediate",
                        choices=["beginner", "intermediate", "advanced"])
    parser.add_argument("--grade-level", default="university",
                        choices=["high_school", "university", "professional"])
    args = parser.parse_args()

    client = chromadb.HttpClient(
        host=CHROMADB_HOST, port=CHROMADB_PORT,
        settings=Settings(anonymized_telemetry=False),
    )
    embeddings = OpenAIEmbeddings(model="text-embedding-v3")

    # 获取或创建 collection
    try:
        collection = client.get_collection(args.subject)
    except Exception:
        collection = client.create_collection(args.subject)

    splitter = RecursiveCharacterTextSplitter(
        chunk_size=512, chunk_overlap=50,
    )

    doc_dir = args.dir
    if not os.path.isdir(doc_dir):
        print(f"目录不存在: {doc_dir}")
        sys.exit(1)

    for filename in os.listdir(doc_dir):
        if not filename.endswith((".md", ".txt")):
            continue
        filepath = os.path.join(doc_dir, filename)
        with open(filepath, "r", encoding="utf-8") as f:
            content = f.read()

        chunks = splitter.split_text(content)
        for i, chunk in enumerate(chunks):
            topic = filename.replace(".md", "").replace(".txt", "")
            doc_id = f"{args.subject}_{topic}_{i}"
            embedding = embeddings.embed_query(chunk)
            collection.add(
                ids=[doc_id],
                embeddings=[embedding],
                documents=[chunk],
                metadatas=[{
                    "subject": args.subject,
                    "topic": topic,
                    "difficulty": args.difficulty,
                    "grade_level": args.grade_level,
                    "content_type": "concept",
                    "title": topic,
                    "source": "builtin",
                }],
            )
        print(f"  已导入: {filename} ({len(chunks)} 个分块)")

    print(f"完成! 共导入到 collection: {args.subject}")


if __name__ == "__main__":
    main()
```

- [ ] **Step 2: 提交**

```bash
git add scripts/ingest_knowledge.py
git commit -m "feat: add knowledge ingestion script for ChromaDB"
```

---

### 任务 17: Python 侧异步持久化消息

**文件:**
- 修改: `ai-planner-service/app/chat/agent.py`
- 可能涉及: `ai-planner-service/app/mq/producer.py`（已有 `publish_result`，可复用）

- [ ] **Step 1: 在 agent 的 done 事件前发送 RabbitMQ 消息**

在 `agent.py` 的 `run_chat_agent` 函数中，`yield {"type": "done"}` 之前，发送一条 RabbitMQ 消息到 `chat.message` 队列:

```python
# 在 yield done 之前添加
try:
    from app.mq.producer import publish_chat_message
    publish_chat_message(user_id, conversation_id)
except Exception as e:
    logger.error(f"Failed to publish chat message: {e}")
```

- [ ] **Step 2: 在 producer.py 中添加 publish_chat_message 函数**

```python
def publish_chat_message(user_id: int, conversation_id: str):
    """发布聊天消息到 RabbitMQ，由 Java 侧消费写入 MySQL"""
    import json
    import pika
    
    credentials = pika.PlainCredentials(RABBITMQ_USER, RABBITMQ_PASS)
    connection = pika.BlockingConnection(
        pika.ConnectionParameters(
            host=RABBITMQ_HOST, port=RABBITMQ_PORT, credentials=credentials
        )
    )
    channel = connection.channel()
    channel.exchange_declare(exchange=AI_EXCHANGE, exchange_type="direct", durable=True)
    
    payload = json.dumps({
        "userId": user_id,
        "conversationId": conversation_id,
        "role": "assistant",
        "content": "",  # 实际内容已存 Redis，这里只是触发持久化
    }, ensure_ascii=False)
    
    channel.basic_publish(
        exchange=AI_EXCHANGE,
        routing_key="chat.message",
        body=payload.encode("utf-8"),
        properties=pika.BasicProperties(delivery_mode=2, content_type="application/json"),
    )
    connection.close()
```

注意：这里有个设计问题 — 消息内容已经在 agent 流式生成时通过 `save_message` 存到 Redis。但 `publish_chat_message` 触发 Java 侧写 MySQL 时需要内容。更好的做法是把完整消息传过来：

修改为:
```python
def publish_chat_message(user_id: int, conversation_id: str,
                         role: str, content: str):
    """发布聊天消息到 RabbitMQ，由 Java 侧消费写入 MySQL"""
    # ... 同上，payload 改为:
    payload = json.dumps({
        "userId": user_id,
        "conversationId": conversation_id,
        "role": role,
        "content": content,
    }, ensure_ascii=False)
```

然后在 agent 中分别对用户消息和助手回复各发一次。

- [ ] **Step 3: 提交**

```bash
git add ai-planner-service/app/chat/agent.py ai-planner-service/app/mq/producer.py
git commit -m "feat: add async chat message persistence via RabbitMQ"
```

---

## 验证清单

全部任务完成后，逐项验证:

- [ ] MySQL `chat_message` 表存在
- [ ] `POST /api/planner/ai/chat` 返回 SSE 流（可通过 curl 测试）
- [ ] SSE 事件类型完整：token / tool_call / done / error
- [ ] 对话历史写入 Redis 并可读取
- [ ] RabbitMQ `chat.message` 消息被 Java Consumer 正确消费并写入 MySQL
- [ ] `GET /api/planner/chat/history?conversationId=x&offset=0` 返回分页数据
- [ ] ChromaDB 可正常连接和检索
- [ ] Tools 调用 planner-service REST API 正常
- [ ] Gateway 路由 `/api/planner/ai/chat` 正确转发到 Python 服务
