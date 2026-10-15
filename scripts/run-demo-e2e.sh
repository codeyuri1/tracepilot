#!/usr/bin/env bash
set -euo pipefail

# Each run owns a new project; existing showcase containers and volumes are untouched.
project="investigator-e2e-$(date +%s)-$$"
compose=(docker compose -p "$project" -f compose.demo.yml --profile showcase --profile e2e)
export API_PORT=0 DASHBOARD_PORT=0
mkdir -p artifacts/demo
cleanup() {
  status=$?
  trap - EXIT
  # Logs may contain submitted payloads; publish only the allowlisted runner artifacts.
  "${compose[@]}" down --volumes --remove-orphans >/dev/null 2>&1 || true
  exit "$status"
}
trap cleanup EXIT

"${compose[@]}" up --build -d --wait postgres app dashboard
# --no-deps avoids starting the legacy single-event producer; the runner loads all fixtures itself.
"${compose[@]}" run --build --rm --no-deps demo-runner
# Check that nginx serves both the UI and the same-origin API proxy.
"${compose[@]}" exec -T dashboard wget -q -O /dev/null http://127.0.0.1/
"${compose[@]}" exec -T dashboard wget -q -O /dev/null http://127.0.0.1/local/demo-token?role=INCIDENT_READER
