# Constituição do projeto

## Princípios não negociáveis

1. **Evidência antes de conclusão.** Uma recomendação de IA precisa apontar para trecho do stack trace, incidente anterior, documentação ou saída de tool. Ausência de evidência deve ser apresentada como incerteza.
2. **Java controla os efeitos.** O LLM sugere classificação e investigação; somente serviços Java chamam banco, arquivos, logs e integrações.
3. **Segurança por padrão.** Não guardar segredos, tokens ou payloads completos de produção sem redaction; tools têm entrada validada, timeout e trilha de auditoria.
4. **Contrato primeiro.** APIs versionadas, DTOs separados de entidades e erros RFC 9457. Mudança incompatível requer nova versão.
5. **Testável localmente.** Regras de negócio em serviços puros quando possível; integração com PostgreSQL via Testcontainers; modelos externos substituíveis por doubles nos testes.
6. **Observável e mensurável.** Cada análise tem correlation id, modelo, latência, custo/tokens quando disponível, fontes e resultado das tools. Retrieval e respostas têm evals de regressão.

## Limites iniciais

O MVP aceita texto enviado manualmente. Não lê logs de produção, não executa comandos no host e não aplica correções. Autenticação, Kafka e deploy são incrementos posteriores.
