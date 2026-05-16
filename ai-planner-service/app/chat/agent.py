import os
import json
import logging
import uuid
from typing import AsyncIterator
from langchain_openai import ChatOpenAI
from langchain_core.messages import HumanMessage, SystemMessage, AIMessage, ToolMessage
from app.chat.rag import retrieve_context
from app.chat.tools import (
    list_user_goals, get_goal_phases, get_mastery_result, create_learning_goal,
)
from app.chat.conversation import load_history, save_message
from app.prompts.templates import CHAT_SYSTEM_PROMPT

logger = logging.getLogger(__name__)

CHAT_MODEL = os.getenv("CHAT_MODEL", "deepseek-v4-pro")

TOOLS = [list_user_goals, get_goal_phases, get_mastery_result, create_learning_goal]


def _build_system_prompt(user_id: int) -> str:
    """构建系统 prompt，包含用户上下文"""
    try:
        from app.memory.user_memory import get_user_summary
        summary = get_user_summary(user_id)
        user_context = json.dumps(summary, ensure_ascii=False)
    except Exception:
        user_context = "暂无历史学习数据"
    return CHAT_SYSTEM_PROMPT.format(user_context=user_context)


async def run_chat_agent(user_id: int, user_message: str,
                         conversation_id: str = None,
                         auth_header: str = None) -> AsyncIterator[dict]:
    """运行聊天 Agent，流式返回事件"""
    if not conversation_id:
        conversation_id = str(uuid.uuid4())

    try:
        from app.mq.producer import publish_chat_message
    except ImportError:
        publish_chat_message = None

    llm = ChatOpenAI(model=CHAT_MODEL, temperature=0.7, streaming=True)
    llm_with_tools = llm.bind_tools(TOOLS)

    # 加载历史和 RAG
    history = load_history(user_id, conversation_id)
    rag_docs = retrieve_context(user_message)
    system_prompt = _build_system_prompt(user_id)

    # 拼装 RAG 上下文
    rag_text = ""
    if rag_docs:
        rag_text = "\n\n相关学习资料：\n"
        for doc in rag_docs:
            title = doc.get("metadata", {}).get("title", "资料")
            rag_text += f"- [{title}] {doc['content'][:200]}\n"

    # 构建消息列表
    messages = [SystemMessage(content=system_prompt + rag_text)]
    for h in history:
        if h["role"] == "user":
            messages.append(HumanMessage(content=h["content"]))
        else:
            messages.append(AIMessage(content=h["content"]))
    messages.append(HumanMessage(content=user_message))

    # 保存用户消息
    save_message(user_id, conversation_id, "user", user_message)
    if publish_chat_message:
        try:
            publish_chat_message(user_id, conversation_id, "user", user_message)
        except Exception as e:
            logger.error(f"Failed to publish chat message: {e}")

    full_response = ""

    try:
        # 第一轮：LLM 决策
        response = await llm_with_tools.ainvoke(messages)

        # 检查是否需要调用工具
        if response.tool_calls:
            messages.append(response)
            for tool_call in response.tool_calls:
                tool_name = tool_call["name"]
                tool_args = tool_call["args"]
                yield {"type": "tool_call", "tool": tool_name, "args": tool_args}

                # 执行工具
                tool_func = {
                    "list_user_goals": list_user_goals,
                    "get_goal_phases": get_goal_phases,
                    "get_mastery_result": get_mastery_result,
                    "create_learning_goal": create_learning_goal,
                }.get(tool_name)

                if tool_func:
                    if tool_name == "list_user_goals":
                        args = {"auth_header": auth_header}
                    else:
                        args = {**tool_args, "auth_header": auth_header}
                    tool_result = await tool_func.ainvoke(args)

                    messages.append(ToolMessage(content=tool_result, tool_call_id=tool_call["id"]))

            # 第二轮：基于工具结果生成回复
            stream_llm = ChatOpenAI(model=CHAT_MODEL, temperature=0.7, streaming=True)
            async for chunk in stream_llm.astream(messages):
                if chunk.content:
                    full_response += chunk.content
                    yield {"type": "token", "content": chunk.content}
        else:
            # 无工具调用，流式返回
            stream_llm = ChatOpenAI(model=CHAT_MODEL, temperature=0.7, streaming=True)
            async for chunk in stream_llm.astream(messages):
                if chunk.content:
                    full_response += chunk.content
                    yield {"type": "token", "content": chunk.content}

    except Exception as e:
        logger.error(f"Chat agent error: {e}", exc_info=True)
        yield {"type": "error", "content": f"处理出错: {str(e)}"}

    # 保存助手回复
    if full_response:
        save_message(user_id, conversation_id, "assistant", full_response)
        if publish_chat_message:
            try:
                publish_chat_message(user_id, conversation_id, "assistant", full_response)
            except Exception as e:
                logger.error(f"Failed to publish chat message: {e}")

    # 通知完成
    yield {"type": "done", "conversation_id": conversation_id}
