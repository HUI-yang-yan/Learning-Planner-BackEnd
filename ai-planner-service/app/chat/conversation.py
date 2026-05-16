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
