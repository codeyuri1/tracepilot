#!/usr/bin/env bash
set -euo pipefail
root="$(cd "$(dirname "$0")/.." && pwd)"; cd "$root"
run_id="$(date -u +%Y%m%dT%H%M%SZ)-ollama"; output="artifacts/ollama/$run_id"; mkdir -p "$output"
compose=(docker compose -p tracepilot-llm -f compose.demo.yml -f compose.llm.yml --profile showcase)
trap '"${compose[@]}" down --remove-orphans >/dev/null 2>&1 || true' EXIT
"${compose[@]}" up --build -d app
for _ in $(seq 1 60); do curl -fsS http://127.0.0.1:18080/actuator/health >/dev/null && break; sleep 1; done
curl -fsS http://127.0.0.1:18080/actuator/health >/dev/null
token() { curl -fsS "http://127.0.0.1:18080/local/demo-token?role=$1&tenant=ollama-demo" | sed -E 's/.*"accessToken":"([^"]+)".*/\1/'; }
writer="$(token INCIDENT_WRITER)"; analyst="$(token ANALYST)"
incident="$(curl -fsS -X POST http://127.0.0.1:18080/api/v1/incidents -H "Authorization: Bearer $writer" -H 'Content-Type: application/json' --data '{"title":"Worker de pagamentos não conecta ao banco","source":"payment-worker","severity":"HIGH","stackTrace":"org.postgresql.util.PSQLException: Connection refused"}')"
incident_id="$(printf '%s' "$incident" | sed -E 's/.*"id":"([^"]+)".*/\1/')"
analysis="$(curl -fsS -X POST "http://127.0.0.1:18080/api/v1/incidents/$incident_id/analyses" -H "Authorization: Bearer $analyst")"
printf '%s\n' "$analysis" > "$output/analysis.json"; printf '%s\n' "$incident" > "$output/incident.json"
grep -q '"summary"' "$output/analysis.json"; grep -q '"probableCause"' "$output/analysis.json"; grep -q 'Connection refused' "$output/analysis.json"
printf '{"model":"%s","status":"validated"}\n' "${AI_MODEL:-llama3.2}" > "$output/summary.json"
