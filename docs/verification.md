# Evidências de verificação — 15/10/2026

Validação executada neste workspace, com Java 21 e Docker. Node 24 foi usado em container. Não há declaração de cobertura de 100%, teste de carga de produção ou certificação de segurança. A revisão posterior da interface está em [UI/UX](ui-ux-review.md).

## Resultados

| Verificação | Comando / evidência | Resultado |
|---|---|---|
| Backend e empacotamento | `./mvnw --batch-mode verify` | **49 testes**, zero falhas/erros/skip; BUILD SUCCESS |
| Frontend | `docker build --target build -t investigator-frontend-tests frontend` | **17 testes**, TypeScript e Vite aprovados (após revisão UI/UX) |
| Gerador de massa | `python3 -m unittest discover -s scripts -p 'test_*.py'` | **3 testes** aprovados |
| Runner e relatórios | `python3 -m unittest discover -s demo/runner -p 'test_*.py'` | **2 testes** aprovados, incluindo servidor HTTP local real |
| Aceitação em containers | `./scripts/run-demo-e2e.sh` | Exit 0; **27 registros aprovados**, zero falhas, incluindo readiness e agregação do fluxo |
| Navegador real | `uv run --with playwright python scripts/browser-smoke.py --screenshot docs/presentation/dashboard.png` | Cenários, análise, filtros, teclado, exportação, temas, falha/retry e mobile aprovados; axe sem violações nos quatro estados auditados |
| Auditoria npm | `docker run --rm investigator-frontend-tests npm audit --audit-level=high` | `found 0 vulnerabilities` no momento da consulta |
| Massa adicional | Gerador com `--count 1000 --seed 42` | 1.000 eventos gerados; 30 eventos da massa versionada ingeridos no E2E |
| Restrições do backend | `docker inspect` | `user=app`, `readonly=true`, `CapDrop=[ALL]`, `no-new-privileges:true` |
| Overrides opcionais | `docker compose ... config --quiet` para LLM e observabilidade | Configurações válidas |

Snapshot versionável: [resumo E2E](verification/e2e-summary.json), [JUnit](verification/e2e-junit.xml) e [captura da interface](presentation/dashboard.png). A execução final gerou também `artifacts/demo/20261015-232022-a74c19d8/`, ignorado pelo Git. Cada nova execução produz um diretório próprio.

Os 27 registros incluem 25 cenários do fluxo HTTP, um readiness e um registro agregador. Fora deles, o shell verificou HTML e proxy Nginx e terminou com exit 0. São verificações funcionais, não uma medida de cobertura de linhas. O frontend foi testado em Node dentro do Docker e no Chromium contra a aplicação real.

## Problemas reproduzidos e corrigidos

- Credenciais JSON, Basic, URLs com senha e e-mails não eram todos redigidos. A sanitização agora ocorre na entrada de incidentes e documentos, antes da persistência e dos embeddings, com defesa adicional na auditoria.
- Segredos entre aspas e com quebras de linha escapavam da redaction. Entradas de 49 mil caracteres também causavam estouro de pilha e busca excessivamente lenta. Há regressão com limite de execução de três segundos; os padrões agora têm repetição sem backtracking nessa região e limites de tamanho em scheme/e-mail.
- O driver PostgreSQL rejeitava `Instant` sem tipo SQL explícito. O adapter agora fornece `Timestamp`.
- As consultas RAG comparavam `text[]` com `varchar[]`; agora convertem o parâmetro ACL para `text[]`.
- Deduplicação global impedia outro tenant de indexar o mesmo conteúdo. O hash de documento agora inclui o escopo ACL; a migração V5 permite hashes de chunks repetidos entre documentos e posições.
- Tools assíncronas perdiam o contexto de segurança e consultavam o tenant `public`. O executor agora propaga o contexto de segurança e o limpa após a chamada.
- A demo reconhecia somente conexão recusada. Os cinco cenários do produtor agora têm respostas determinísticas com evidências literais.
- O dashboard não carregava o histórico inicialmente nem após o simulador concluir a análise. Também permitia trocar de incidente durante uma análise pendente. Os testes cobrem esses comportamentos.
- O runner anterior cobria somente parte do que a documentação afirmava. O runner atual usa parsing JSON, executa a massa pela API, verifica os controles anunciados e produz JUnit com casos individuais.
- O E2E usava estado compartilhado entre execuções e o CI não verificava frontend. Agora há isolamento por projeto, limpeza somente dos recursos temporários próprios e verificação backend/frontend/ferramentas/E2E.

As regressões relevantes foram observadas falhando antes das respectivas correções. A revisão independente encontrou quatro problemas adicionais; foram corrigidos e revisados novamente sem novos achados relevantes.

O primeiro teste real revelou `confidence` textual incompatível com o contrato numérico. O prompt agora inclui o JSON Schema tipado e exige valor de 0 a 1; um teste de regressão verifica esse contrato. O modelo real passou após a correção, sem fallback para respostas determinísticas.

## Sanitização do material de portfólio

Uma varredura pontual de 173 arquivos textuais em código, frontend, demo, scripts, docs, CI e observabilidade não encontrou os padrões procurados de chave privada, AWS access key, token GitHub ou chave OpenAI. Ela não exibiu valores encontrados e não inspecionou dados privados do ambiente.

Esse resultado é limitado a esses padrões, não equivale a uma auditoria completa. As credenciais de demonstração do Compose continuam explícitas e documentadas. Os artefatos E2E registram somente nomes de cenário, método, caminho sem query e status; não incluem autorização, corpo HTTP, texto de exceção ou logs brutos. A captura usa dados sintéticos.

## Limites e operação

- Ollama real validado via `/v1/chat/completions`, modelo `qwen2.5-coder:7b`: diagnóstico completo em 4,44 s numa chamada aquecida, evidência literal e três recomendações. Essa medição isolada não é benchmark. A suíte E2E continua determinística para ser reproduzível; embeddings permanecem determinísticos neste ambiente. A interface também foi verificada no Chromium exibindo o modelo e o selo “LLM real”.
- Grafana e Prometheus iniciados: health OK, datasource consultado com sucesso, target `up=1` e dashboard “TracePilot — Operação” provisionado. O endpoint de métricas do backend foi exercitado com e sem papel OPS.
- O workflow foi editado e seus comandos principais foram executados localmente; não houve execução remota do GitHub Actions.
- Maven apresenta avisos existentes de instrumentação dinâmica do Mockito/Byte Buddy em Java 21; a suíte conclui com exit 0.
- O Git local foi inicializado para publicação, preservando o commit MIT existente no remoto. Arquivos privados e evidências brutas estão excluídos por `.gitignore`; os arquivos selecionados para o commit foram revisados separadamente. A varredura não certifica ausência de todos os tipos de segredo.
- A migração V5 preserva dados e remove apenas a unicidade global do hash de chunk. O novo hash ACL passa a valer para novas ingestões; não reescreve hashes ou sanitiza conteúdo histórico.
- A showcase final ficou no projeto Compose `investigator-validation`: dashboard em `http://localhost:8081`, API em `http://localhost:18080` e Grafana em `http://localhost:3000`. O comando com os três arquivos Compose e perfis está no README, seção de LLM real.

Kafka e processamento assíncrono permanecem evolução de arquitetura, sem promessa de runtime existente. A demo local não deve ser publicada com o emissor de tokens e as credenciais de exemplo.
