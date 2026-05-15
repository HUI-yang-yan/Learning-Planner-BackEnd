import json
import os
import logging
from datetime import datetime, timedelta
import redis

logger = logging.getLogger(__name__)

REDIS_HOST = os.getenv("REDIS_HOST", "192.168.171.135")
REDIS_PORT = int(os.getenv("REDIS_PORT", "6379"))
REDIS_PASSWORD = os.getenv("REDIS_PASSWORD", "redis123")

MEMORY_TTL_DAYS = 90  # Keep memory for 90 days


def _get_redis():
    return redis.Redis(
        host=REDIS_HOST, port=REDIS_PORT, password=REDIS_PASSWORD,
        decode_responses=True, socket_connect_timeout=3
    )


def save_goal_memory(user_id: int, goal_id: int, goal_name: str,
                     goal_type: str, difficulty: str, phases: list):
    """Save completed goal analysis to user memory"""
    try:
        r = _get_redis()
        key = f"user:{user_id}:goals"
        memory = {
            "goal_id": goal_id,
            "goal_name": goal_name,
            "goal_type": goal_type,
            "difficulty": difficulty,
            "phase_count": len(phases),
            "timestamp": datetime.now().isoformat(),
        }
        r.lpush(key, json.dumps(memory, ensure_ascii=False))
        r.ltrim(key, 0, 49)  # Keep last 50 goals
        r.expire(key, MEMORY_TTL_DAYS * 86400)
        r.close()
        logger.info(f"Goal memory saved: user={user_id}, goal={goal_id}")
    except Exception as e:
        logger.error(f"Failed to save goal memory: {e}")


def get_user_history(user_id: int) -> list[dict]:
    """Retrieve user's learning history for context"""
    try:
        r = _get_redis()
        key = f"user:{user_id}:goals"
        data = r.lrange(key, 0, -1)
        r.close()
        return [json.loads(item) for item in data]
    except Exception as e:
        logger.error(f"Failed to get user history: {e}")
        return []


def save_weakness_memory(user_id: int, weaknesses: list[str]):
    """Record user's weak areas"""
    try:
        r = _get_redis()
        key = f"user:{user_id}:weaknesses"
        for w in weaknesses:
            r.sadd(key, w)
        r.expire(key, MEMORY_TTL_DAYS * 86400)
        r.close()
        logger.info(f"Weakness memory updated: user={user_id}, weaknesses={weaknesses}")
    except Exception as e:
        logger.error(f"Failed to save weakness memory: {e}")


def get_user_weaknesses(user_id: int) -> list[str]:
    """Get user's historical weak areas"""
    try:
        r = _get_redis()
        key = f"user:{user_id}:weaknesses"
        result = list(r.smembers(key))
        r.close()
        return result
    except Exception as e:
        logger.error(f"Failed to get weaknesses: {e}")
        return []


def save_progress_snapshot(user_id: int, goal_id: int,
                           completed_tasks: int, total_tasks: int,
                           mastery_score: float):
    """Save learning progress snapshot"""
    try:
        r = _get_redis()
        key = f"user:{user_id}:progress"
        snapshot = {
            "goal_id": goal_id,
            "completed_tasks": completed_tasks,
            "total_tasks": total_tasks,
            "completion_rate": round(completed_tasks / max(total_tasks, 1) * 100, 1),
            "mastery_score": mastery_score,
            "timestamp": datetime.now().isoformat(),
        }
        r.lpush(key, json.dumps(snapshot, ensure_ascii=False))
        r.ltrim(key, 0, 99)
        r.expire(key, MEMORY_TTL_DAYS * 86400)
        r.close()
        logger.info(f"Progress snapshot saved: user={user_id}")
    except Exception as e:
        logger.error(f"Failed to save progress snapshot: {e}")


def get_user_summary(user_id: int) -> dict:
    """Get user learning summary for context in AI prompts"""
    history = get_user_history(user_id)
    weaknesses = get_user_weaknesses(user_id)

    goal_types = list(set(h.get("goal_type", "") for h in history if h.get("goal_type")))

    return {
        "total_goals": len(history),
        "goal_types": goal_types,
        "weaknesses": weaknesses,
        "recent_goals": [h.get("goal_name", "") for h in history[:5]],
    }
