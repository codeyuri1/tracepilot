# 001 — Registrar e consultar incidentes

## Objetivo

Como engenheiro de suporte, quero registrar um erro com seu stack trace para que ele tenha um identificador estável e possa ser investigado posteriormente.

## Contrato HTTP

`POST /api/v1/incidents` recebe `title`, `stackTrace`, `source` e `severity` (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`). Retorna `201 Created`, header `Location` e um `IncidentResponse` cujo status inicial é `RECEIVED`.

`GET /api/v1/incidents/{id}` retorna o incidente ou `404`.

## Regras

- `title` tem 5–180 caracteres; `stackTrace`, 20–50.000; `source`, 2–100.
- A API nunca inclui stack trace em respostas de erro.
- `createdAt` é definido pelo servidor em UTC e não pode ser alterado pelo cliente.
- Persistência é transacional: ou o incidente inteiro existe, ou não existe.

## Critérios de aceite

- [ ] POST válido cria registro e retorna todos os campos públicos esperados.
- [ ] Entrada inválida retorna `400 application/problem+json` com violações por campo.
- [ ] ID inexistente retorna `404 application/problem+json`.
- [ ] Um teste de integração prova que o registro permanece após a transação terminar.

## Fora de escopo

Analisar LLM, similaridade, autenticação, paginação e upload de arquivos.
