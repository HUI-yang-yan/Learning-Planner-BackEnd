import os
import logging
import chromadb
from chromadb.config import Settings
from langchain_openai import OpenAIEmbeddings

logger = logging.getLogger(__name__)

CHROMADB_HOST = os.getenv("CHROMADB_HOST", "192.168.171.135")
CHROMADB_PORT = os.getenv("CHROMADB_PORT", "8001")
EMBEDDING_MODEL = os.getenv("EMBEDDING_MODEL", "text-embedding-v3")

_embeddings = None
_chroma_client = None


def _get_embeddings():
    global _embeddings
    if _embeddings is None:
        _embeddings = OpenAIEmbeddings(
            model=EMBEDDING_MODEL,
            api_key=os.getenv("OPENAI_API_KEY"),
            base_url=os.getenv("OPENAI_BASE_URL"),
        )
    return _embeddings


def _get_chroma_client():
    global _chroma_client
    if _chroma_client is None:
        _chroma_client = chromadb.HttpClient(
            host=CHROMADB_HOST,
            port=CHROMADB_PORT,
            settings=Settings(anonymized_telemetry=False),
        )
    return _chroma_client


def retrieve_context(query: str, subject: str = None,
                     difficulty: str = None, top_k: int = 5) -> list[dict]:
    """从 ChromaDB 检索相关知识文档"""
    client = _get_chroma_client()
    embeddings = _get_embeddings()

    collection_names = [subject] if subject else _list_collections(client)
    all_docs = []

    for coll_name in collection_names:
        try:
            collection = client.get_collection(coll_name)
        except Exception:
            continue

        where_filter = {}
        if difficulty:
            where_filter["difficulty"] = difficulty

        query_embedding = embeddings.embed_query(query)
        results = collection.query(
            query_embeddings=[query_embedding],
            n_results=top_k,
            where=where_filter if where_filter else None,
        )

        if results and results.get("metadatas") and results["metadatas"][0]:
            for i, metadata in enumerate(results["metadatas"][0]):
                all_docs.append({
                    "content": results["documents"][0][i] if results.get("documents") else "",
                    "metadata": metadata,
                })
        if len(all_docs) >= top_k:
            break

    # 截断到 top_k
    return all_docs[:top_k]


def _list_collections(client) -> list[str]:
    """获取所有 collection 名称"""
    try:
        return [c.name for c in client.list_collections()]
    except Exception:
        return []
