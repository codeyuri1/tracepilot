# 004 — Investigação por agent e tools

## Objetivo

Permitir que o modelo solicite dados adicionais através de ferramentas estreitamente definidas, sem ganhar acesso irrestrito ao ambiente.

## Tools iniciais

| Tool | Entrada | Saída | Restrição |
|---|---|---|---|
| `searchSimilarIncidents` | consulta, topK | IDs, score, excerto | somente dados autorizados |
| `searchDocumentation` | consulta, produto | fontes, excertos | somente base indexada |
| `inspectStackTrace` | incidentId | frames normalizados | somente incidente atual |

`searchLogs` só entra após definir fonte, retenção, mascaramento e autorização. `generateDiagram` não executa renderizadores arbitrários; retorna Mermaid validado.

## Critérios de aceite

- [ ] Tool calls têm schema, validação, timeout, correlation id e registro de auditoria.
- [ ] O modelo não consegue escolher nome de tool fora da allowlist nem fornecer parâmetros extras.
- [ ] Repetição da mesma chamada no mesmo ciclo usa cache e não duplica efeito.
- [ ] Limite de chamadas e orçamento de tokens encerram a investigação de forma explicável.

## Plano técnico

`InvestigationOrchestrator` é uma máquina de estados e recebe um `ToolRegistry` explícito. Adaptadores Spring AI chamam funções Java; tools não conhecem `ChatClient`. Resultados são devolvidos ao modelo como dados não confiáveis, delimitados no prompt.
