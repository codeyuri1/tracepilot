# 002 — Análise estruturada por IA

## Objetivo

Ao solicitar a análise de um incidente, o usuário recebe uma hipótese útil, estruturada e explicitamente limitada às evidências disponíveis.

## Contrato HTTP

`POST /api/v1/incidents/{id}/analyses` inicia uma análise síncrona no MVP e retorna `201` com: `category`, `summary`, `probableCause`, `confidence` (0–1), `evidence[]`, `recommendations[]`, `model` e `promptVersion`.

## Regras

- O prompt instrui o modelo a não alegar acesso a código, logs ou incidentes ainda indisponíveis.
- Structured output é validado no Java. Saída inválida, timeout ou indisponibilidade cria análise `FAILED`, sem apagar o incidente.
- Cada evidência precisa ter `type`, `reference` e `excerpt`; o `excerpt` deve ocorrer no material fornecido ao modelo.
- `confidence` é uma estimativa do modelo, não uma probabilidade estatística.

## Critérios de aceite

- [ ] Double determinístico do modelo prova a conversão para o contrato de análise.
- [ ] Saída sem evidência válida é rejeitada e marcada como falha de validação.
- [ ] Prompt e resposta redigidos são auditáveis por `analysisId`.
- [ ] Endpoint não expõe chave de provedor nem prompt de sistema.
