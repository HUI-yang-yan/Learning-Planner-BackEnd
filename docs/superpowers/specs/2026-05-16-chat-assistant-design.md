# Chat Assistant Design Spec

**Date:** 2026-05-16  
**Status:** Approved

## Overview

Add a conversational learning assistant to the existing `ai-planner-service`. The assistant helps users by answering learning questions, providing learning path advice, and creating/managing learning goals — all through natural conversation. It uses RAG for knowledge retrieval and can call Tools to interact with the planner-service.

## Key Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Architecture | Extend ai-planner-service | Shared LLM config, Redis memory, RabbitMQ; no duplicate infra |
| Agent Framework | LangGraph Agent + `bind_tools` | Already using LangChain/LangGraph; single-round tool decisions |
| RAG Vector DB | ChromaDB (already deployed, 192.168.171.135:8001) | Lightweight, LangChain-native, sufficient for current scale |
| Embedding | DashScope text-embedding-v3 | Same API provider as LLM |
| Streaming | SSE (Server-Sent Events) | One-way stream, session-isolated, FastAPI-native |
| Conversation Storage | MySQL persistent + Redis page cache | Reliable persistence, frontend progressive loading (20/page) |
| Tool Execution | Python HTTP → Java REST endpoints | Synchronous response needed for agent tool chain |

## Project Structure (New Files)

```
ai-planner-service/app/
├── chat/
│   ├── __init__.py
│   ├── agent.py              # LangGraph chat agent with bind_tools
│   ├── tools.py              # Tool definitions (HTTP calls to Java)
│   ├── rag.py                # ChromaDB retriever with metadata filtering
│   └── conversation.py       # Conversation history helper
├── api/
│   └── chat.py               # SSE chat endpoint + history endpoint
├── prompts/templates.py      # Add: CHAT_SYSTEM_PROMPT
└── models/schemas.py         # Add: ChatRequest, ChatEvent

planner-service/
├── controller/ChatHistoryController.java  # GET /api/planner/chat/history
├── mq/ChatMessageConsumer.java            # Consume chat.message → write MySQL
├── entity/ChatMessage.java (common)
├── repository/ChatMessageMapper.java
sql/
└── V3__add_chat_message.sql

scripts/
└── ingest_knowledge.py       # Batch import knowledge docs into ChromaDB
```

## API

### POST /api/planner/ai/chat

Request:
```json
{
  "message": "我学 Python 卡在 pandas 了怎么办",
  "conversationId": "uuid-optional"
}
```

Response: `text/event-stream`
```
data: {"type":"token","content":"pandas"}

data: {"type":"token","content":" 的核心..."}

data: {"type":"tool_call","tool":"get_mastery_result","args":{"goalId":1}}

data: {"type":"done","conversationId":"generated-uuid"}
```

### GET /api/planner/chat/history

```
GET /api/planner/chat/history?conversationId={id}&offset=0
```

Response:
```json
{
  "code": 200,
  "data": {
    "items": [...],        // 20 messages
    "hasMore": true,
    "nextOffset": 20
  }
}
```

## Data Flow

```
1. User → POST /api/planner/ai/chat {"message": "..."}
2. Load conversation history from Redis (chat:history:{uid}:{cid}:recent)
   If miss → MySQL → populate Redis
3. ChromaDB retrieve top-5 relevant docs via metadata-filtered similarity search
4. Build context: system_prompt + history + rag_docs + user_message
5. LangGraph Agent (LLM + bind_tools):
   a. Decides: answer directly OR call tool(s)
   b. If tool → HTTP call Java → feed result back → continue
6. Stream tokens via SSE
7. On complete: send {conversationId, role, content} → RabbitMQ chat.message
   → Java ChatMessageConsumer → INSERT MySQL
   → Invalidate Redis cache for that page
```

## Conversation Storage

### MySQL Table

```sql
CREATE TABLE chat_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    conversation_id VARCHAR(36) NOT NULL,
    role VARCHAR(10) NOT NULL COMMENT 'user | assistant',
    content TEXT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_conv_page (conversation_id, id)
);
```

### Redis Cache

| Key Pattern | Value | TTL |
|-------------|-------|-----|
| `chat:history:{uid}:{cid}:{page}` | JSON list of messages | 10 min |
| `chat:history:{uid}:{cid}:recent` | Last 20 messages (for agent context) | 1 hour |

- Page size: 20 messages
- Frontend loads page 0 on enter, requests page 1 on scroll up
- Cache miss → MySQL LIMIT 20 OFFSET N → write cache → return

## Tools

### Read Tools

| Tool | Calls | Returns |
|------|-------|---------|
| `list_user_goals` | `GET /api/planner/goals` | User's learning goals |
| `get_goal_phases` | `GET /api/planner/phases/page?goalId=` | Phases for a goal |
| `get_mastery_result` | `GET /api/planner/ai/mastery/result/{goalId}` | Latest evaluation |

### Write Tools

| Tool | Calls | Returns |
|------|-------|---------|
| `create_learning_goal` | `POST /api/planner/goals` | Created goal (triggers async AI analysis) |

- Tools use `httpx.AsyncClient` for HTTP calls to Java planner-service
- Service discovery via Nacos host resolution or Docker container name
- Auth: forward Sa-Token from incoming request

## RAG Knowledge Base

### Collections (by subject)

```
chroma_collections/
├── computer_science/
├── mathematics/
├── english/
└── ...
```

### Document Metadata

```python
{
    "subject": "Python",          # top-level category
    "topic": "pandas",            # sub-topic
    "difficulty": "beginner",     # beginner / intermediate / advanced
    "grade_level": "university",  # high_school / university / professional
    "content_type": "concept",    # concept / tutorial / exercise / faq
    "title": "Pandas DataFrame 基础操作",
    "source": "builtin"           # builtin / user_uploaded
}
```

### Retrieval

- Top-5 documents via ChromaDB similarity search
- Metadata filters derived from user context (difficulty, grade_level from user profile)
- Documents chunked at 512 tokens, overlap 50

### Ingestion

```bash
python scripts/ingest_knowledge.py --subject computer_science --dir ./docs/cs/
```

## New Dependencies

### Python (requirements.txt additions)

```
chromadb==0.5.20
httpx==0.27.2       # async HTTP for tool calls
```

### Environment Variables (additions)

```
CHROMADB_HOST=192.168.171.135
CHROMADB_PORT=8001
EMBEDDING_MODEL=text-embedding-v3
CHAT_MAX_HISTORY=20
CHAT_MODEL=deepseek-v4-pro
```

### Java

- `common/entity/ChatMessage.java`
- `planner-service/repository/ChatMessageMapper.java`
- `planner-service/controller/ChatHistoryController.java`
- `planner-service/mq/ChatMessageConsumer.java`
- `sql/V3__add_chat_message.sql`
