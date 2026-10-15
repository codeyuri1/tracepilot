# 005 — Qualidade, segurança e operação

## Objetivo

Demonstrar que o sistema melhora de modo mensurável e pode operar sem vazar dados ou esconder falhas.

## Critérios de aceite

- [ ] Evals versionados medem classificação, qualidade da recuperação, fidelidade de fontes e correção de tool calls.
- [ ] Alterações de prompt/modelo são comparadas com baseline e têm limite de regressão definido.
- [ ] Actuator/Micrometer expõem latência, erros, uso de tools, tamanho de contexto e métricas de retrieval, sem conteúdo sensível.
- [ ] Autenticação JWT protege endpoints; autorização restringe incidentes, documentos e tools por tenant/role.
- [ ] Testes unitários, web e integração rodam no CI; PostgreSQL real usa Testcontainers.
- [ ] Eventos de análise assíncrona são introduzidos somente após contrato idempotente e DLQ definido.

## Plano técnico

Adicionar Spring Security como resource server, OpenTelemetry/Micrometer e uma camada de redaction. Para cargas longas, publicar evento transacional `IncidentAnalysisRequested`; Kafka é opção de transporte, não dependência inicial.
