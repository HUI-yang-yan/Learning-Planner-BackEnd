import json
import logging
from langchain_openai import ChatOpenAI
from langchain_core.messages import HumanMessage
from app.prompts.templates import ROADMAP_GENERATOR_PROMPT

logger = logging.getLogger(__name__)


def generate_phases(state: dict) -> dict:
    """Generate learning phases based on goal analysis"""
    llm = ChatOpenAI(
        model=state.get("model", "gpt-4o-mini"),
        temperature=0.3,
    )
    analysis = state["analysis"]
    prompt = ROADMAP_GENERATOR_PROMPT.format(
        goal_name=state["goal_name"],
        goal_type=analysis.get("goal_type", ""),
        difficulty=analysis.get("difficulty", ""),
        estimated_duration=analysis.get("estimated_duration", ""),
        required_skills=", ".join(analysis.get("required_skills", [])),
    )
    response = llm.invoke([HumanMessage(content=prompt)])
    content = response.content.strip()
    if content.startswith("```json"):
        content = content.removeprefix("```json").removesuffix("```").strip()
    elif content.startswith("```"):
        content = content.removeprefix("```").removesuffix("```").strip()
    phases = json.loads(content)
    logger.info(f"Phases generated: {len(phases)} phases")
    return {"phases": phases}
