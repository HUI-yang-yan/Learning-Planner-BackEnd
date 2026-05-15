import json
import os
import logging
import pika

logger = logging.getLogger(__name__)

RABBITMQ_HOST = os.getenv("RABBITMQ_HOST", "192.168.171.135")
RABBITMQ_PORT = int(os.getenv("RABBITMQ_PORT", "5672"))
RABBITMQ_USER = os.getenv("RABBITMQ_USER", "guest")
RABBITMQ_PASS = os.getenv("RABBITMQ_PASS", "guest")
AI_EXCHANGE = "ai.exchange"
AI_RESULT_QUEUE = "ai.result.queue"


def publish_result(result: dict):
    """Publish AI generated result back to Java"""
    credentials = pika.PlainCredentials(RABBITMQ_USER, RABBITMQ_PASS)
    connection = pika.BlockingConnection(
        pika.ConnectionParameters(
            host=RABBITMQ_HOST, port=RABBITMQ_PORT, credentials=credentials
        )
    )
    channel = connection.channel()
    channel.exchange_declare(exchange=AI_EXCHANGE, exchange_type="direct", durable=True)
    channel.queue_declare(queue=AI_RESULT_QUEUE, durable=True)
    channel.queue_bind(
        queue=AI_RESULT_QUEUE, exchange=AI_EXCHANGE, routing_key="ai.result"
    )
    channel.basic_publish(
        exchange=AI_EXCHANGE,
        routing_key="ai.result",
        body=json.dumps(result, ensure_ascii=False).encode("utf-8"),
        properties=pika.BasicProperties(
            delivery_mode=2,
            content_type="application/json",
        ),
    )
    connection.close()
    logger.info(f"Published result for goal_id={result['goal_id']}")
