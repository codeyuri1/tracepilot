# 003 — Recuperação de conhecimento (RAG)

## Objetivo

Enriquecer uma análise com incidentes anteriores e documentação relevante, sempre com fonte verificável.

## Regras e critérios de aceite

- [ ] Documentos são normalizados, fragmentados com sobreposição configurável e têm metadados de origem, versão e ACL.
- [ ] Embeddings são armazenados no PostgreSQL com pgvector; a dimensão é validada na ingestão.
- [ ] Recuperação retorna top-k com score, fonte e trecho; somente resultados acima de limiar seguem ao LLM.
- [ ] Busca híbrida (vetorial + lexical) e reranking são comparados por conjunto de evals versionado.
- [ ] Uma resposta não usa fonte sem ACL do solicitante.

## Plano técnico

`KnowledgeIngestionService → Chunker → EmbeddingPort → knowledge_chunk`; `Retriever → HybridRanker → RerankerPort`. O contexto ao modelo contém IDs de fonte, não documentos inteiros. Começar com pgvector e PostgreSQL full-text search antes de introduzir infraestrutura adicional.
