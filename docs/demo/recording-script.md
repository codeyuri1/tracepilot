# Roteiro de gravação — TracePilot

Tempo alvo: dois minutos. Use somente o incidente sintético em `portfolio-walkthrough.http`.

1. Mostre o README e a arquitetura: API, domínio/portas, PostgreSQL/pgvector, modelo e tools controladas.
2. Inicie `docker compose up -d postgres` e a aplicação no profile `local`.
3. Execute as quatro primeiras requisições do walkthrough no IntelliJ HTTP Client ou equivalente.
4. Mostre que `stackTrace` não está na resposta pública de criação/consulta e que o `X-Correlation-Id` é devolvido.
5. Mostre a análise bem-sucedida com um modelo configurado ou, sem modelo, o `503 analysis-unavailable`; explique que não há diagnóstico inventado.
6. Abra `/swagger-ui/index.html`, `/actuator/health` e uma métrica autenticada como `OPS`.
7. Feche com os testes, migrations, ADRs, specs e contrato de evento em `docs/events/`.

Não grave tokens, chaves, dados reais, respostas de produção ou o valor de `APP_SECURITY_JWT_SECRET`.
