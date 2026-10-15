# Plano técnico

Manter a ingestão separada da consulta. Cada chunk recebe `documentId`, `chunkIndex`, conteúdo, hash, metadados e embedding. O hash torna reingestão idempotente. A query combina PostgreSQL full-text search e distância vetorial, depois aplica reranking apenas ao conjunto candidato; isso controla custo e latência.

A primeira avaliação é offline, com perguntas e fontes esperadas revisadas manualmente. Só após uma baseline existir, alterar chunk size, embedding ou reranker.
