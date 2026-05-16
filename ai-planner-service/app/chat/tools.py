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
