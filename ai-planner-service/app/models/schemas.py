from pydantic import BaseModel, Field
from typing import Optional


class GoalAnalysisRequest(BaseModel):
    goal_id: int
    user_id: int
    goal_name: str
    goal_desc: str = ""


class GoalAnalysis(BaseModel):
    goal_type: str
    difficulty: str
    estimated_duration: str
    required_skills: list[str]


class TaskItem(BaseModel):
    task_name: str
    task_desc: str
    priority: int = 1
    estimated_hours: int = 1


class PhaseItem(BaseModel):
    phase_name: str
    phase_order: int
    phase_desc: str
    estimated_days: int
    tasks: list[TaskItem]


class RoadmapResult(BaseModel):
    goal_id: int
    goal_type: str
    difficulty: str
    estimated_duration: str
    required_skills: list[str]
    phases: list[PhaseItem]


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
