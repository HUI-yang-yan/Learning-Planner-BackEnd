import json
import os
import logging
import pika
from app.agent.workflow import run_planner_workflow
from app.mq.producer import publish_result

logger = logging.getLogger(__name__)

RABBITMQ_HOST = os.getenv("RABBITMQ_HOST", "192.168.171.135")
RABBITMQ_PORT = int(os.getenv("RABBITMQ_PORT", "5672"))
RABBITMQ_USER = os.getenv("RABBITMQ_USER", "guest")
RABBITMQ_PASS = os.getenv("RABBITMQ_PASS", "guest")
AI_EXCHANGE = "ai.exchange"
GOAL_ANALYSIS_QUEUE = "goal.analysis.queue"


def on_message(ch, method, properties, body):
    """Handle goal analysis request"""
    try:
        msg = json.loads(body)
        logger.info(f"Received goal analysis request: {msg}")
        result = run_planner_workflow(
            goal_id=msg["goalId"],
            user_id=msg["userId"],
            goal_name=msg["goalName"],
            goal_desc=msg.get("goalDesc", ""),
        )
        publish_result(result.model_dump())
        ch.basic_ack(delivery_tag=method.delivery_tag)
        logger.info(f"Goal {msg['goalId']} processed successfully")
    except Exception as e:
        logger.error(f"Failed to process message: {e}", exc_info=True)
        ch.basic_nack(delivery_tag=method.delivery_tag, requeue=True)


MASTERY_EVALUATE_QUEUE = "mastery.evaluate.queue"


def on_mastery_message(ch, method, properties, body):
    """Handle mastery evaluation request"""
    try:
        msg = json.loads(body)
        logger.info(f"Received mastery evaluation request: {msg}")
        from app.agent.mastery_evaluator import evaluate_mastery
        result = evaluate_mastery(
            goal_id=msg["goalId"],
            goal_name=msg.get("goalName", ""),
            completion_rate=msg.get("completionRate", 0),
            learning_hours=msg.get("learningHours", 0),
            test_score=msg.get("testScore", 0),
            phases_summary=msg.get("phasesSummary", ""),
        )
        # Publish result back
        from app.mq.producer import publish_mastery_result
        publish_mastery_result(result, msg["goalId"])
        ch.basic_ack(delivery_tag=method.delivery_tag)
        logger.info(f"Mastery evaluation for goal {msg['goalId']} completed")
    except Exception as e:
        logger.error(f"Failed to process mastery request: {e}", exc_info=True)
        ch.basic_nack(delivery_tag=method.delivery_tag, requeue=True)


def start_consumer():
    """Start RabbitMQ consumer"""
    credentials = pika.PlainCredentials(RABBITMQ_USER, RABBITMQ_PASS)
    connection = pika.BlockingConnection(
        pika.ConnectionParameters(
            host=RABBITMQ_HOST, port=RABBITMQ_PORT, credentials=credentials
        )
    )
    channel = connection.channel()
    channel.exchange_declare(exchange=AI_EXCHANGE, exchange_type="direct", durable=True)

    # Goal analysis queue
    channel.queue_declare(queue=GOAL_ANALYSIS_QUEUE, durable=True)
    channel.queue_bind(
        queue=GOAL_ANALYSIS_QUEUE, exchange=AI_EXCHANGE, routing_key="goal.analysis"
    )
    channel.basic_consume(queue=GOAL_ANALYSIS_QUEUE, on_message_callback=on_message)

    # Mastery evaluation queue
    channel.queue_declare(queue=MASTERY_EVALUATE_QUEUE, durable=True)
    channel.queue_bind(
        queue=MASTERY_EVALUATE_QUEUE, exchange=AI_EXCHANGE, routing_key="mastery.evaluate"
    )
    channel.basic_consume(queue=MASTERY_EVALUATE_QUEUE, on_message_callback=on_mastery_message)

    channel.basic_qos(prefetch_count=1)
    logger.info(f"Listening on queues: {GOAL_ANALYSIS_QUEUE}, {MASTERY_EVALUATE_QUEUE}")
    channel.start_consuming()
