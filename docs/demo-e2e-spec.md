# Demo End-to-End Specification

## Goal

Provide a reproducible containerized demonstration that simulates technical log intake and proves the incident, analysis, RAG, controlled-tool, security and observability flows with synthetic data only.

## Scenario

`log-producer` emits a deterministic payment-worker failure. `demo-runner` obtains short-lived local JWTs, ingests the event, waits for the generated incident, ingests a synthetic runbook and a prior incident into the knowledge base, requests an analysis, validates retrieval/tool outcomes and downloads public observability signals. The application uses deterministic local model and embedding adapters, so the scenario never calls OpenAI, Ollama or production services.

## Acceptance criteria

- `docker compose --profile demo up --build --abort-on-container-exit demo-runner` executes without manual API calls.
- The scenario covers JWT/RBAC, tenant isolation, log-to-incident normalization, lifecycle persistence, redacted audit, successful structured analysis, invalid-analysis failure, knowledge ingestion, hybrid retrieval, allowed/rejected/cached tool calls, correlation IDs, and protected/public Actuator endpoints.
- Every input is synthetic, versioned and contains no access token, API key, password or customer data.
- The runner writes a machine-readable `summary.json`, JUnit XML and sanitized request/response evidence to a mounted `artifacts/demo/` directory.
- CI runs the same Compose scenario and uploads the artifacts even on failure.
- The human-facing result is a short generated Markdown report linked from the README and a GitHub Actions artifact named `demo-e2e-report`.

## Non-goals

- Connecting to a real Kafka cluster, log platform, LLM provider, identity provider or production database.
- Treating the demo token endpoint as a production authentication design.
- Load/performance benchmarking; this is a behavioral, evidence-backed demonstration suite.

## Delivery

Local: `artifacts/demo/<run-id>/report.md`, `summary.json`, `junit.xml`, `requests.ndjson`, and Compose logs.

CI: GitHub Actions job `demo-e2e` uploads the same directory as `demo-e2e-report`; the workflow job summary links the report and lists pass/fail scenarios.
