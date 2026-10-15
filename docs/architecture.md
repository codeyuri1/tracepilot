# Arquitetura alvo

```text
HTTP API → application use cases → domain ports ← infrastructure adapters
                 │                    │
                 ├─ Incident/JPA       ├─ Spring AI (Ollama/OpenAI)
                 ├─ pgvector/RAG       └─ controlled Java tools
                 └─ observability/evals
```

## Pacotes

```text
com.codeyuri.incidentinvestigator
├── api/             # controllers, DTOs e ProblemDetail
├── application/     # casos de uso e orquestração
│   └── usecase/
├── domain/          # invariantes, modelos e portas sem frameworks
│   ├── model/
│   └── port/
├── infrastructure/  # adapters: JPA, Spring AI, pgvector e segurança
│   └── persistence/jpa/
└── evaluation/      # datasets, métricas e executor de evals
```

Dependências apontam para dentro: `api` e `infrastructure` dependem de `application`; `application` depende de `domain`; `domain` não depende de Spring, JPA ou SDK de IA.

Consulte a [ADR 0002](decisions/0002-domain-driven-design-boundaries.md) para as regras de colocação de código e a migração do legado inicial.
