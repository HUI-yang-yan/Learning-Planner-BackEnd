import os
import json
import logging
import redis

logger = logging.getLogger(__name__)

REDIS_HOST = os.getenv("REDIS_HOST", "192.168.171.135")
REDIS_PORT = int(os.getenv("REDIS_PORT", "6379"))
REDIS_PASSWORD = os.getenv("REDIS_PASSWORD", "redis123")
STREAM_CHANNEL = "ai-stream-channel"


def publish_progress(goal_id: int, step: str, message: str):
    """Publish AI progress to Redis for WebSocket broadcasting"""
    try:
        r = redis.Redis(
            host=REDIS_HOST,
            port=REDIS_PORT,
            password=REDIS_PASSWORD,
            decode_responses=True,
        )
        payload = json.dumps({
            "goalId": goal_id,
            "step": step,
            "content": message,
        }, ensure_ascii=False)
        r.publish(STREAM_CHANNEL, payload)
        r.close()
        logger.info(f"Published progress: goal={goal_id}, step={step}")
    except Exception as e:
        logger.error(f"Failed to publish progress: {e}")
