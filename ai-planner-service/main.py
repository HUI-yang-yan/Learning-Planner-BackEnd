import os
import logging
from dotenv import load_dotenv

load_dotenv()

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)
logger = logging.getLogger("ai-planner")

from app.mq.consumer import start_consumer

if __name__ == "__main__":
    logger.info("Starting AI Planner Service...")
    start_consumer()
