from pydantic import BaseModel, Field
from pydantic.alias_generators import to_camel
from typing import Optional


class CamelModel(BaseModel):
    """自动将 snake_case 字段名序列化为 camelCase，兼容 Java 侧"""
    model_config = {"alias_generator": to_camel, "populate_by_name": True}


class GoalAnalysisRequest(CamelModel):
    goal_id: int
    user_id: int
    goal_name: str
    goal_desc: str = ""


class GoalAnalysis(CamelModel):
    goal_type: str
    difficulty: str
    estimated_duration: str
    required_skills: list[str]


class TaskItem(CamelModel):
    task_name: str
    task_desc: str
    priority: int = 1
    estimated_hours: int = 1


class PhaseItem(CamelModel):
    phase_name: str
    phase_order: int
    phase_desc: str
    estimated_days: int
    tasks: list[TaskItem]


class RoadmapResult(CamelModel):
    goal_id: int
    goal_type: str
    difficulty: str
    estimated_duration: str
    required_skills: list[str]
    phases: list[PhaseItem]


class ChatRequest(CamelModel):
    message: str
    conversation_id: str | None = None


class ToolCallEvent(CamelModel):
    type: str = "tool_call"
    tool: str
    args: dict


class TokenEvent(CamelModel):
    type: str = "token"
    content: str


class DoneEvent(CamelModel):
    type: str = "done"
    conversation_id: str
