import json
import os
import logging
import pika

logger = logging.getLogger(__name__)

RABBITMQ_HOST = os.getenv("RABBITMQ_HOST", "192.168.171.135")
RABBITMQ_PORT = int(os.getenv("RABBITMQ_PORT", "5672"))
RABBITMQ_USER = os.getenv("RABBITMQ_USER", "admin")
RABBITMQ_PASS = os.getenv("RABBITMQ_PASS", "admin123")
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


def publish_mastery_result(result: dict, goal_id: int):
    """Publish mastery evaluation result back to Java"""
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
    payload = {
        "goalId": goal_id,
        "masteryScore": result.get("mastery_score", 0),
        "weaknesses": result.get("weaknesses", []),
        "suggestions": result.get("suggestions", []),
        "shouldAdjust": result.get("should_adjust", False),
    }
    channel.basic_publish(
        exchange=AI_EXCHANGE,
        routing_key="ai.result",
        body=json.dumps(payload, ensure_ascii=False).encode("utf-8"),
        properties=pika.BasicProperties(
            delivery_mode=2,
            content_type="application/json",
        ),
    )
    connection.close()
    logger.info(f"Published mastery result for goal_id={goal_id}")


def publish_chat_message(user_id: int, conversation_id: str,
                         role: str, content: str):
    """发布聊天消息到 RabbitMQ，由 Java 侧消费写入 MySQL"""
    credentials = pika.PlainCredentials(RABBITMQ_USER, RABBITMQ_PASS)
    connection = pika.BlockingConnection(
        pika.ConnectionParameters(
            host=RABBITMQ_HOST, port=RABBITMQ_PORT, credentials=credentials
        )
    )
    channel = connection.channel()
    channel.exchange_declare(exchange=AI_EXCHANGE, exchange_type="direct", durable=True)
    channel.queue_declare(queue="chat.message.queue", durable=True)
    channel.queue_bind(
        queue="chat.message.queue", exchange=AI_EXCHANGE, routing_key="chat.message"
    )
    payload = json.dumps({
        "userId": user_id,
        "conversationId": conversation_id,
        "role": role,
        "content": content,
    }, ensure_ascii=False)
    channel.basic_publish(
        exchange=AI_EXCHANGE,
        routing_key="chat.message",
        body=payload.encode("utf-8"),
        properties=pika.BasicProperties(
            delivery_mode=2,
            content_type="application/json",
        ),
    )
    connection.close()
    logger.info(f"Published chat message: convId={conversation_id}, role={role}")
