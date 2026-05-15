import json
import logging
from langchain_openai import ChatOpenAI
from langchain_core.messages import HumanMessage
from app.prompts.templates import GOAL_ANALYZER_PROMPT

logger = logging.getLogger(__name__)


def analyze_goal(state: dict) -> dict:
    """Analyze user goal, return type, difficulty, estimated time, required skills"""
    llm = ChatOpenAI(
        model=state.get("model", "gpt-4o-mini"),
        temperature=0.3,
    )
    prompt = GOAL_ANALYZER_PROMPT.format(
        goal_name=state["goal_name"],
        goal_desc=state.get("goal_desc", ""),
    )
    response = llm.invoke([HumanMessage(content=prompt)])
    content = response.content.strip()
    if content.startswith("```json"):
        content = content.removeprefix("```json").removesuffix("```").strip()
    elif content.startswith("```"):
        content = content.removeprefix("```").removesuffix("```").strip()
    analysis = json.loads(content)
    logger.info(f"Goal analysis complete: type={analysis.get('goal_type')}")
    return {"analysis": analysis}
