from unittest.mock import patch, MagicMock
from app.chat.rag import retrieve_context


@patch("app.chat.rag._get_chroma_client")
@patch("app.chat.rag._get_embeddings")
def test_retrieve_context_returns_docs(mock_emb, mock_chroma):
    mock_collection = MagicMock()
    mock_collection.query.return_value = {
        "documents": [["Python 是一门解释型语言..."]],
        "metadatas": [[{"subject": "Python", "difficulty": "beginner"}]],
    }
    mock_client = MagicMock()
    mock_client.get_collection.return_value = mock_collection
    mock_chroma.return_value = mock_client
    mock_emb.return_value.embed_query.return_value = [0.1] * 1024

    result = retrieve_context("什么是 Python")
    assert len(result) == 1
    assert "Python" in result[0]["content"]
