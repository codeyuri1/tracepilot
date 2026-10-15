# ADR 0002 — Limites DDD e scaffold de camadas

**Status:** aceito

## Contexto

O primeiro incremento foi iniciado com entidades JPA no pacote `domain`. Isso encurta a entrega inicial, mas mistura regras de negócio com detalhes de persistência e contradiz a arquitetura alvo: o domínio não pode depender de Spring, JPA ou SDKs externos.

## Decisão

O projeto adotará o seguinte scaffold para todo código novo e para a migração progressiva do código existente:

```text
domain/
  model/       # agregados, entidades, value objects e invariantes Java puros
  port/        # contratos que o domínio/aplicação requer de integrações externas
application/
  usecase/     # orquestração de comandos e consultas; limites transacionais
api/           # HTTP, DTOs e ProblemDetail
infrastructure/
  persistence/
    jpa/       # entidades JPA, Spring Data e mapeadores
  ai/          # adaptadores Spring AI
```

As dependências apontam para dentro. `api` e `infrastructure` podem depender de `application` e `domain`; `application` depende de `domain`; `domain` depende apenas do JDK.

O mapeamento entre o modelo de domínio e entidades JPA será feito por adapters de persistência. Controladores nunca expõem entidades de domínio ou de persistência diretamente. Novos casos de uso recebem portas por construtor; adaptadores Spring AI, banco de dados e configurações por profile ficam em `infrastructure`.

## Consequências

- Uma feature começa pelo modelo e pelas portas em `domain`, antes de adapters ou controllers.
- Anotações `jakarta.persistence.*`, `org.springframework.*` e SDKs de provedores são proibidas em novos arquivos sob `domain/model` e `domain/port`.
- As entidades JPA atuais sob `domain` são dívida técnica identificada e serão migradas para `infrastructure/persistence/jpa` antes do incremento 003, sem alteração do contrato HTTP.
- Testes de regra de domínio não iniciam Spring; testes de adapters usam Testcontainers/doubles conforme necessário.
