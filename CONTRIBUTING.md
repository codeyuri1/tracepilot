# Contribuindo com o TracePilot

## Branches e commits

Trabalhe em `feat/`, `fix/`, `docs/`, `chore/`, `refactor/`, `test/`, `ci/`, `perf/`, `build/` ou `revert/`. Use commits Conventional Commits, por exemplo `feat: adicionar investigação guiada`. A branch de destino é `main`.

## Pull requests automáticos

Ao enviar uma branch com um desses prefixos, o workflow **Automatic PR** cria um PR para `main` caso ainda não exista. Novos pushes atualizam o mesmo PR. Não crie um segundo PR manualmente. O primeiro commit da branch fornece o título e deve seguir Conventional Commits.

A automação usa `GITHUB_TOKEN`, com `contents: read` e `pull-requests: write`. Em **Settings → Actions → General → Workflow permissions**, habilite **Allow GitHub Actions to create and approve pull requests**. Não é necessário fornecer PAT. O workflow não aprova PRs nem faz merge automaticamente.

## Regras de revisão

- Título em Conventional Commits, descrição da mudança e evidências de validação.
- CI e PR Policy aprovados; corrija falhas antes de integrar.
- Sem credenciais reais, dados privados ou artefatos locais. Use somente exemplos e massa sintética.
- Mudanças visuais devem apresentar captura e considerar teclado e acessibilidade.
- Preserve compatibilidade e descreva migrações, riscos e alterações de contrato.
- Prefira squash merge, mantendo o título do PR como mensagem do commit.

Os workflows validam essas regras, mas não substituem proteção da branch. Para bloqueá-las no servidor, configure um ruleset de `main`: exigir PR, checks `verify`, `demo-e2e` e `PR Policy`, resolver conversas e bloquear force-push/deleção. Não exija aprovação de outro usuário se o repositório for mantido individualmente. O merge continua explícito.

## Verificações locais

```bash
./mvnw --batch-mode verify
python3 -m unittest discover -s scripts -p 'test_*.py'
python3 -m unittest discover -s demo/runner -p 'test_*.py'
(cd frontend && npm ci && npm test && npm run build)
./scripts/run-demo-e2e.sh
```

Os testes de integração exigem Docker. `.gitignore` exclui dependências, builds, evidências brutas, modelos, dados de runtime e estado privado de ferramentas. Capturas e relatórios sintéticos selecionados em `docs/` permanecem versionados. Os contextos Docker incluem somente arquivos necessários para construir cada imagem.
