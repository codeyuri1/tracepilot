# Plano técnico

Evals rodam como suíte separada e usam fixtures versionadas; não chamam produção. Métricas usam tags de baixa cardinalidade (`operation`, `outcome`, `model`, `tool`) e nunca incluem stack trace, prompt ou ID de usuário.

JWT entra com escopos mínimos. Multi-tenancy é aplicado no serviço/repositório, não confiado apenas ao filtro HTTP. Para eventos, utilizar outbox transacional antes de escolher Kafka como transporte.
