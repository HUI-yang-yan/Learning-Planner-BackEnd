import json
import logging
from langchain_openai import ChatOpenAI
from langchain_core.messages import HumanMessage
from app.prompts.templates import MASTERY_EVALUATOR_PROMPT
from app.redis.publisher import publish_progress

logger = logging.getLogger(__name__)


def evaluate_mastery(goal_id: int, goal_name: str, completion_rate: float,
                     learning_hours: float, test_score: float,
                     phases_summary: str = "", model: str = "gpt-4o-mini") -> dict:
    """Evaluate user mastery and return assessment with suggestions"""
    publish_progress(goal_id, "EVALUATING_MASTERY", "Analyzing learning progress...")

    llm = ChatOpenAI(model=model, temperature=0.3)
    prompt = MASTERY_EVALUATOR_PROMPT.format(
        goal_name=goal_name,
        completion_rate=completion_rate,
        learning_hours=learning_hours,
        test_score=test_score,
        phases_summary=phases_summary,
    )
    response = llm.invoke([HumanMessage(content=prompt)])
    content = response.content.strip()
    if content.startswith("```json"):
        content = content.removeprefix("```json").removesuffix("```").strip()
    elif content.startswith("```"):
        content = content.removeprefix("```").removesuffix("```").strip()
    result = json.loads(content)

    publish_progress(goal_id, "MASTERY_DONE",
                     f"Mastery score: {result.get('mastery_score', 0)}%")
    logger.info(f"Mastery evaluation complete: score={result.get('mastery_score')}")
    return result
