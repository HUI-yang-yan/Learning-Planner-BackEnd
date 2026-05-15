import logging
from langgraph.graph import StateGraph, END
from typing import TypedDict, Optional
from app.agent.goal_analyzer import analyze_goal
from app.agent.roadmap_generator import generate_phases
from app.agent.task_splitter import split_tasks
from app.models.schemas import RoadmapResult

logger = logging.getLogger(__name__)


class PlannerState(TypedDict):
    goal_id: int
    user_id: int
    goal_name: str
    goal_desc: str
    model: str
    analysis: Optional[dict]
    phases: Optional[list[dict]]


def build_workflow() -> StateGraph:
    workflow = StateGraph(PlannerState)

    workflow.add_node("goal_analyzer", analyze_goal)
    workflow.add_node("roadmap_generator", generate_phases)
    workflow.add_node("task_splitter", split_tasks)

    workflow.set_entry_point("goal_analyzer")
    workflow.add_edge("goal_analyzer", "roadmap_generator")
    workflow.add_edge("roadmap_generator", "task_splitter")
    workflow.add_edge("task_splitter", END)

    return workflow.compile()


def run_planner_workflow(goal_id: int, user_id: int,
                         goal_name: str, goal_desc: str = "",
                         model: str = "gpt-4o-mini") -> RoadmapResult:
    """Run the complete planning workflow"""
    logger.info(f"Starting planner workflow for goal_id={goal_id}: {goal_name}")
    app = build_workflow()
    result = app.invoke({
        "goal_id": goal_id,
        "user_id": user_id,
        "goal_name": goal_name,
        "goal_desc": goal_desc,
        "model": model,
        "analysis": None,
        "phases": None,
    })
    analysis = result["analysis"]
    phases = result["phases"]
    logger.info(f"Workflow complete: {len(phases)} phases generated")
    return RoadmapResult(
        goal_id=goal_id,
        goal_type=analysis.get("goal_type", ""),
        difficulty=analysis.get("difficulty", "intermediate"),
        estimated_duration=analysis.get("estimated_duration", ""),
        required_skills=analysis.get("required_skills", []),
        phases=phases,
    )
