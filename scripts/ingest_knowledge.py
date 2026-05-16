#!/usr/bin/env python3
"""将 Markdown 知识文档批量导入 ChromaDB"""
import os
import sys
import argparse
import chromadb
from chromadb.config import Settings
from langchain_openai import OpenAIEmbeddings
from langchain_text_splitters import RecursiveCharacterTextSplitter

CHROMADB_HOST = os.getenv("CHROMADB_HOST", "192.168.171.135")
CHROMADB_PORT = os.getenv("CHROMADB_PORT", "8001")

# 通过环境变量加载 llm key/base_url
from dotenv import load_dotenv
load_dotenv()


def main():
    parser = argparse.ArgumentParser(description="导入知识文档到 ChromaDB")
    parser.add_argument("--subject", required=True, help="学科名，作为 collection 名称")
    parser.add_argument("--dir", required=True, help="Markdown 文档目录")
    parser.add_argument("--difficulty", default="intermediate",
                        choices=["beginner", "intermediate", "advanced"])
    parser.add_argument("--grade-level", default="university",
                        choices=["high_school", "university", "professional"])
    args = parser.parse_args()

    client = chromadb.HttpClient(
        host=CHROMADB_HOST, port=CHROMADB_PORT,
        settings=Settings(anonymized_telemetry=False),
    )
    embeddings = OpenAIEmbeddings(model="text-embedding-v3")

    # 获取或创建 collection
    try:
        collection = client.get_collection(args.subject)
    except Exception:
        collection = client.create_collection(args.subject)

    splitter = RecursiveCharacterTextSplitter(
        chunk_size=512, chunk_overlap=50,
    )

    doc_dir = args.dir
    if not os.path.isdir(doc_dir):
        print(f"目录不存在: {doc_dir}")
        sys.exit(1)

    for filename in os.listdir(doc_dir):
        if not filename.endswith((".md", ".txt")):
            continue
        filepath = os.path.join(doc_dir, filename)
        with open(filepath, "r", encoding="utf-8") as f:
            content = f.read()

        chunks = splitter.split_text(content)
        for i, chunk in enumerate(chunks):
            topic = filename.replace(".md", "").replace(".txt", "")
            doc_id = f"{args.subject}_{topic}_{i}"
            embedding = embeddings.embed_query(chunk)
            collection.add(
                ids=[doc_id],
                embeddings=[embedding],
                documents=[chunk],
                metadatas=[{
                    "subject": args.subject,
                    "topic": topic,
                    "difficulty": args.difficulty,
                    "grade_level": args.grade_level,
                    "content_type": "concept",
                    "title": topic,
                    "source": "builtin",
                }],
            )
        print(f"  已导入: {filename} ({len(chunks)} 个分块)")

    print(f"完成! 共导入到 collection: {args.subject}")


if __name__ == "__main__":
    main()
