# Especificações

Cada diretório representa uma fatia vertical. O ciclo obrigatório é:

1. atualizar `spec.md` com comportamento observável e critérios de aceite;
2. registrar decisões técnicas em `plan.md`;
3. quebrar o trabalho em itens verificáveis em `tasks.md`;
4. escrever o teste que demonstra cada critério antes da implementação;
5. só marcar uma tarefa concluída com comando de verificação registrado.

| Ordem | Incremento | Resultado de negócio |
|---|---|---|
| 001 | Incident intake | registrar, consultar e classificar o estado de um incidente |
| 002 | AI analysis | devolver diagnóstico estruturado, sem inventar evidências |
| 003 | Knowledge retrieval | recuperar incidentes e documentos semelhantes com fontes |
| 004 | Agent tools | permitir investigação limitada por ferramentas auditáveis |
| 005 | Quality & operations | medir qualidade, segurança e operação do sistema |

Não se deve implementar uma spec posterior como atalho para concluir uma anterior.
