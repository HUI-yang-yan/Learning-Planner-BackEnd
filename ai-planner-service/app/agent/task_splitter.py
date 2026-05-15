import json
import logging
from langchain_openai import ChatOpenAI
from langchain_core.messages import HumanMessage
from app.prompts.templates import TASK_SPLITTER_PROMPT

logger = logging.getLogger(__name__)


def split_tasks(state: dict) -> dict:
    """Split tasks for each learning phase"""
    llm = ChatOpenAI(
        model=state.get("model", "gpt-4o-mini"),
        temperature=0.3,
    )
    phases = state["phases"]
    result_phases = []
    for phase in phases:
        prompt = TASK_SPLITTER_PROMPT.format(
            phase_name=phase["phase_name"],
            phase_desc=phase.get("phase_desc", ""),
            estimated_days=phase.get("estimated_days", 7),
        )
        response = llm.invoke([HumanMessage(content=prompt)])
        content = response.content.strip()
        if content.startswith("```json"):
            content = content.removeprefix("```json").removesuffix("```").strip()
        elif content.startswith("```"):
            content = content.removeprefix("```").removesuffix("```").strip()
        tasks = json.loads(content)
        phase["tasks"] = tasks
        result_phases.append(phase)
        logger.info(f"Tasks generated for phase '{phase['phase_name']}': {len(tasks)} tasks")
    return {"phases": result_phases}
