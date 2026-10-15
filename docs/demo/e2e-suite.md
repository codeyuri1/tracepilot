# Aceitação HTTP da demonstração

```bash
./scripts/run-demo-e2e.sh
```

O script constrói e inicia PostgreSQL/pgvector, backend e dashboard em um projeto Compose exclusivo. Usa portas aleatórias no loopback. O runner Python (somente biblioteca padrão) ingere `demo/fixtures/events.ndjson` pela API; não reutiliza o arquivo compartilhado do antigo produtor de evento único. A aplicação e o simulador permanecem Java.

O runner verifica:

- Saúde, OpenAPI, token ausente/inválido e RBAC de escrita/análise/métricas.
- Ingestão dos 30 eventos, confirmação de LOW/MEDIUM/HIGH e análise dos cinco cenários com histórico e recomendações de investigar, mitigar e validar.
- Incidente, lista e histórico isolados por tenant; análise de outro tenant negada.
- Validação de entrada, redaction nos títulos retornados e rejeição de evidência não fornecida.
- Ingestão/deduplicação de documento, recuperação híbrida e ACL por tenant.
- Allowlist de tools, cache, frames do stack trace e propagação do correlation ID.
- Métricas disponíveis a OPS e ausência do marcador sintético sensível.

Após o runner, o script verifica via HTTP o HTML servido pelo Nginx e o proxy de sessão local. A interação visual tem testes React separados; essa verificação HTTP não equivale a automação de navegador.

`artifacts/demo/<run-id>/` contém `report.md`, `summary.json`, `junit.xml` e `requests.ndjson`. Os registros HTTP usam allowlist de método, caminho sem query e status, sem tokens, headers ou payloads. Falhas individuais têm elementos `testcase/failure` no JUnit e causam exit code diferente de zero.

Um trap encerra somente o projeto criado nesta execução, incluindo seus volumes descartáveis, mesmo em falha. A showcase interativa usa outro projeto e permanece independente. Logs brutos não são publicados. Para depurar, reproduza uma showcase com dados sintéticos e consulte seus logs localmente.

O CI publica os relatórios mesmo em falha. Dependências: Docker/Compose, rede para baixar imagens e espaço em disco para builds. Não requer Kafka, chave paga ou serviço de IA externo.
