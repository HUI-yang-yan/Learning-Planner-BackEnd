import pytest
from unittest.mock import patch, AsyncMock, MagicMock
from app.chat.tools import list_user_goals, create_learning_goal


@pytest.mark.asyncio
@patch("app.chat.tools.httpx.AsyncClient")
async def test_list_user_goals_success(mock_client):
    mock_instance = AsyncMock()
    mock_resp = AsyncMock()
    mock_resp.json.return_value = {"code": 200, "data": {"records": []}}
    mock_resp.raise_for_status = MagicMock()
    mock_instance.get.return_value = mock_resp
    mock_client.return_value.__aenter__.return_value = mock_instance

    result = await list_user_goals.ainvoke({})
    assert "records" in result


@pytest.mark.asyncio
@patch("app.chat.tools.httpx.AsyncClient")
async def test_create_learning_goal_success(mock_client):
    mock_instance = AsyncMock()
    mock_resp = AsyncMock()
    mock_resp.json.return_value = {"code": 200, "data": {}}
    mock_resp.raise_for_status = MagicMock()
    mock_instance.post.return_value = mock_resp
    mock_client.return_value.__aenter__.return_value = mock_instance

    result = await create_learning_goal.ainvoke({"goal_name": "学 Python"})
    assert "已创建" in result
