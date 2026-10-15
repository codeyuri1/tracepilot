# ADR 0001 — Spring Boot 3.5 e Spring AI 1.0

**Status:** aceito

## Contexto

O objetivo de aprendizagem requer Spring Boot 3 e Java 21. A linha Spring AI 2.x é direcionada ao Spring Boot 4, enquanto a documentação do Spring AI 1.0 declara suporte a Spring Boot 3.4 e 3.5.

## Decisão

Usar Spring Boot `3.5.16`, Java `21` e importar o BOM do Spring AI `1.0.9`. O primeiro adapter de modelo será Ollama; OpenAI será um profile separado para não exigir chave local.

## Consequência

Novas APIs do Spring AI 2.x não devem ser copiadas para este código. Uma futura migração para Boot 4/Spring AI 2 requer ADR e testes de compatibilidade.
