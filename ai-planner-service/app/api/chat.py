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
