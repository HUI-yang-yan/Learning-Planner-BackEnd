import logging
from langgraph.graph import StateGraph, END
from typing import TypedDict, Optional
from app.agent.goal_analyzer import analyze_goal
from app.agent.roadmap_generator import generate_phases
from app.agent.task_splitter import split_tasks
from app.models.schemas import RoadmapResult
from app.redis.publisher import publish_progress

logger = logging.getLogger(__name__)


class PlannerState(TypedDict):
    goal_id: int
    user_id: int
    goal_name: str
    goal_desc: str
    model: str
    analysis: Optional[dict]
    phases: Optional[list[dict]]


class ProgressAwareGoalAnalyzer:
    """Wrapper that publishes progress after goal analysis"""
    def __call__(self, state: dict) -> dict:
        goal_id = state["goal_id"]
        publish_progress(goal_id, "ANALYZING", "Analyzing learning goal...")
        result = analyze_goal(state)
        publish_progress(goal_id, "ANALYSIS_DONE", "Goal analysis complete")
        return result


class ProgressAwareRoadmapGenerator:
    """Wrapper that publishes progress after roadmap generation"""
    def __call__(self, state: dict) -> dict:
        goal_id = state["goal_id"]
        publish_progress(goal_id, "GENERATING_ROADMAP", "Generating learning roadmap...")
        result = generate_phases(state)
        publish_progress(goal_id, "ROADMAP_DONE", f"Roadmap generated: {len(result['phases'])} phases")
        return result


class ProgressAwareTaskSplitter:
    """Wrapper that publishes progress during and after task splitting"""
    def __call__(self, state: dict) -> dict:
        goal_id = state["goal_id"]
        publish_progress(goal_id, "SPLITTING_TASKS", "Breaking down tasks for each phase...")
        result = split_tasks(state)
        total_tasks = sum(len(p.get("tasks", [])) for p in result["phases"])
        publish_progress(goal_id, "TASKS_DONE", f"All {total_tasks} tasks generated")
        return result


def build_workflow() -> StateGraph:
    workflow = StateGraph(PlannerState)

    workflow.add_node("goal_analyzer", ProgressAwareGoalAnalyzer())
    workflow.add_node("roadmap_generator", ProgressAwareRoadmapGenerator())
    workflow.add_node("task_splitter", ProgressAwareTaskSplitter())

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
    publish_progress(goal_id, "STARTED", "AI planner started")
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
    publish_progress(goal_id, "COMPLETED", "Learning plan ready")
    logger.info(f"Workflow complete: {len(phases)} phases generated")
    return RoadmapResult(
        goal_id=goal_id,
        goal_type=analysis.get("goal_type", ""),
        difficulty=analysis.get("difficulty", "intermediate"),
        estimated_duration=analysis.get("estimated_duration", ""),
        required_skills=analysis.get("required_skills", []),
        phases=phases,
    )
