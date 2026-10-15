# Roteiro de apresentação no LinkedIn

![Captura da demonstração sintética](dashboard.png)

## Antes de gravar

Execute `./scripts/run-demo-e2e.sh` e confira `failed: 0` no relatório mais recente. Abra a showcase conforme o README. Use somente os dados sintéticos. Feche terminais com variáveis de ambiente, credenciais e tokens antes de capturar a tela.

## Vídeo de 90 segundos

| Tempo | Tela | Mensagem |
|---|---|---|
| 0–15 s | Dashboard com incidentes | “Investigar logs exige contexto. Construí uma API que transforma incidentes em diagnósticos estruturados com evidências verificáveis.” |
| 15–35 s | Selecionar incidente de banco | Mostrar categoria, causa provável e o trecho “Connection refused”. Explicar que a demo usa um modelo determinístico. |
| 35–50 s | Injetar e analisar manualmente | Mostrar o caminho produtor → API → persistência → análise, sem exibir o JWT. |
| 50–65 s | Diagrama do README | Destacar limites entre casos de uso, persistência, IA, segurança e observabilidade. |
| 65–80 s | Relatório E2E | Mostrar casos de RBAC, isolamento de tenant e rejeição de evidência inválida. |
| 80–90 s | Repositório e testes | Encerrar com os trade-offs e a próxima evolução: processamento assíncrono. |

## Texto de post

Construí o TracePilot, um projeto de portfólio em Java 21, Spring Boot e Spring AI para investigar incidentes técnicos com evidências verificáveis.

A API recebe um incidente e retorna categoria, causa provável, recomendações e trechos que precisam existir no material fornecido. Quando a resposta do modelo não passa na validação, a análise falha explicitamente.

O projeto inclui:

- PostgreSQL/pgvector e recuperação híbrida com ACL;
- JWT, papéis e isolamento por tenant;
- sanitização de formatos conhecidos de credenciais antes da persistência;
- tools com allowlist, timeout, orçamento e cache;
- dashboard React, métricas e execução em Docker;
- testes unitários, integração com banco real e aceitação HTTP com massas sintéticas reproduzíveis.

A demo funciona sem chave paga, usando um modelo determinístico. Também há integração opcional com uma LLM compatível com OpenAI. Kafka e processamento assíncrono são próximos passos, ainda não fazem parte do runtime.

Meu foco foi tornar decisões técnicas demonstráveis: além da interface, o repositório traz os contratos, os testes e os limites atuais.

[Adicionar URL pública do repositório e vídeo após revisão.]

#Java #SpringBoot #SpringAI #Docker #PostgreSQL #TestesDeSoftware

## Pontos para discussão técnica

- Por que validar evidências literais não prova que uma causa é verdadeira?
- Como equilibrar remoção de dados sensíveis e preservação do contexto diagnóstico?
- O que a demo determinística comprova e o que exige avaliação com uma LLM real?
- Como projetar idempotência, outbox e DLQ antes de introduzir Kafka?

Não publicar contagens de testes ou métricas de desempenho sem conferir a evidência da versão apresentada. O texto acima não declara certificação de segurança ou prontidão para produção.
