"""HTTP acceptance checks. Artifacts contain only scenario names, paths and status codes."""
import json
import os
import time
import uuid
import xml.etree.ElementTree as ET
from pathlib import Path
from urllib.error import HTTPError
from urllib.request import Request, urlopen


class Runner:
    def __init__(self, base, output, fixtures):
        self.base = base.rstrip('/')
        self.output = output
        self.fixtures = fixtures
        self.scenarios = {}
        self.requests = []

    def request(self, method, path, token=None, payload=None, headers=None):
        request_headers = {'Content-Type': 'application/json', **(headers or {})}
        if token:
            request_headers['Authorization'] = 'Bearer ' + token
        data = json.dumps(payload).encode() if payload is not None else (b'' if method == 'POST' else None)
        request = Request(self.base + path, data=data, headers=request_headers, method=method)
        try:
            response = urlopen(request, timeout=15)
        except HTTPError as error:
            response = error
        with response:
            body = response.read().decode()
            status, response_headers = response.status, dict(response.headers)
        # Never store query values, headers, bodies, or exception text.
        self.requests.append({'method': method, 'path': path.split('?')[0], 'status': status})
        return status, response_headers, body

    def token(self, role, tenant='demo'):
        status, _, body = self.request('GET', f'/local/demo-token?role={role}&tenant={tenant}')
        assert status == 200
        return json.loads(body)['accessToken']

    def expect(self, method, path, status, token=None, payload=None, headers=None):
        actual, response_headers, body = self.request(method, path, token, payload, headers)
        assert actual == status, f'Expected HTTP {status}, got {actual}'
        return response_headers, json.loads(body) if body else None

    def scenario(self, name, action):
        try:
            action()
            self.scenarios[name] = 'passed'
        except Exception:
            self.scenarios[name] = 'failed'
        print(f'{name}: {self.scenarios[name]}', flush=True)

    def run(self):
        ready = False
        for _ in range(60):
            try:
                if self.request('GET', '/actuator/health')[0] == 200:
                    ready = True
                    break
            except Exception:
                pass
            time.sleep(1)
        self.scenarios['health_ready'] = 'passed' if ready else 'failed'
        if not ready:
            return self.finish()
        self.scenario('acceptance_flow', self.acceptance)
        return self.finish()

    def acceptance(self):
        writer = self.token('INCIDENT_WRITER')
        reader = self.token('INCIDENT_READER')
        analyst = self.token('ANALYST')
        other = self.token('INCIDENT_READER', 'other-tenant')
        self.scenario('api_requires_token', lambda: self.expect('GET', '/api/v1/incidents', 401))
        self.scenario('invalid_token_rejected', lambda: self.expect('GET', '/api/v1/incidents', 401, 'invalid'))
        self.scenario('metrics_require_token', lambda: self.expect('GET', '/actuator/prometheus', 401))
        self.scenario('reader_cannot_write', lambda: self.expect('POST', '/api/v1/incidents', 403, reader, {}))
        self.scenario('writer_cannot_analyze', lambda: self.expect('POST', '/api/v1/incidents/00000000-0000-0000-0000-000000000000/analyses', 403, writer))
        self.scenario('invalid_input_rejected', lambda: self.expect('POST', '/api/v1/demo/log-events', 400, writer, {}))
        self.scenario('openapi_available', lambda: self.expect('GET', '/v3/api-docs', 200))
        incident_ids = []

        def ingest_fixtures():
            rows = [json.loads(line) for line in self.fixtures.read_text().splitlines() if line.strip()]
            assert len(rows) >= 5
            severities = set()
            for row in rows:
                _, created = self.expect('POST', '/api/v1/demo/log-events', 201, writer, row)
                incident_ids.append(created['id'])
                expected = {'INFO': 'LOW', 'WARN': 'MEDIUM', 'ERROR': 'HIGH'}[row['level']]
                assert created['severity'] == expected
                severities.add(created['severity'])
                assert 'stackTrace' not in created
            assert severities == {'LOW', 'MEDIUM', 'HIGH'}
        self.scenario('synthetic_mass_ingestion', ingest_fixtures)
        assert incident_ids, 'No incident available'
        incident = '/api/v1/incidents/' + incident_ids[0]
        self.scenario('reader_incident_lookup', lambda: self.expect('GET', incident, 200, reader))
        self.scenario('tenant_incident_hidden', lambda: self.expect('GET', incident, 404, other))
        self.scenario('tenant_analysis_hidden', lambda: self.expect('GET', incident + '/analyses', 404, other))
        self.scenario('tenant_cannot_analyze', lambda: self.expect('POST', incident + '/analyses', 404, self.token('ANALYST', 'other-tenant')))

        def tenant_list():
            _, items = self.expect('GET', '/api/v1/incidents', 200, other)
            assert not any(item['id'] in incident_ids for item in items)
        self.scenario('tenant_list_isolation', tenant_list)

        def analyze_scenarios():
            for identifier in incident_ids[:5]:
                _, result = self.expect('POST', f'/api/v1/incidents/{identifier}/analyses', 201, analyst)
                assert result['evidence'] and 0 <= result['confidence'] <= 1
                assert len(result['recommendations']) == 3
                assert all(action.startswith(prefix) for action, prefix in zip(result['recommendations'], ['Investigar:', 'Mitigar:', 'Validar:']))
            _, history = self.expect('GET', incident + '/analyses', 200, reader)
            assert history and history[0]['evidence']
        self.scenario('five_scenarios_and_analysis_history', analyze_scenarios)

        def invalid_analysis():
            _, created = self.expect('POST', '/api/v1/demo/log-events', 201, writer,
                                     dict(service='negative-worker', level='ERROR', message='Unknown synthetic failure',
                                          stackTrace='demo.synthetic.UnknownException: insufficient evidence'))
            self.expect('POST', f"/api/v1/incidents/{created['id']}/analyses", 503, analyst)
        self.scenario('unsupported_evidence_rejected', invalid_analysis)

        def sanitize(endpoint):
            secret = 'synthetic-sensitive-value'
            stack = 'Connection refused password=' + secret
            if endpoint.endswith('log-events'):
                payload = dict(service='redaction-worker', level='ERROR', message='password=' + secret, stackTrace=stack)
            else:
                payload = dict(title='Connection refused password=' + secret, source='redaction-worker', severity='HIGH', stackTrace=stack)
            _, created = self.expect('POST', endpoint, 201, writer, payload)
            assert secret not in json.dumps(created)
            _, listed = self.expect('GET', '/api/v1/incidents', 200, reader)
            assert secret not in json.dumps(listed)
        self.scenario('log_input_sanitized', lambda: sanitize('/api/v1/demo/log-events'))
        self.scenario('incident_input_sanitized', lambda: sanitize('/api/v1/incidents'))

        source = 'runbook-' + uuid.uuid4().hex
        document = dict(source=source, version='v1', content='PostgreSQL Connection refused: verify database availability and network connectivity.')
        self.scenario('knowledge_ingestion', lambda: self.expect('POST', '/api/v1/demo/knowledge', 201, writer, document))
        self.scenario('knowledge_deduplication', lambda: self.expect('POST', '/api/v1/demo/knowledge', 200, writer, document))

        def retrieve(token, visible):
            _, results = self.expect('GET', '/api/v1/demo/knowledge?query=PostgreSQL', 200, token)
            assert any(result['source'] == source for result in results) == visible
        self.scenario('rag_retrieval', lambda: retrieve(reader, True))
        self.scenario('rag_tenant_isolation', lambda: retrieve(other, False))

        def own_copy():
            copy = {**document, 'source': source + '-other'}
            other_writer = self.token('INCIDENT_WRITER', 'other-tenant')
            self.expect('POST', '/api/v1/demo/knowledge', 201, other_writer, copy)
            _, results = self.expect('GET', '/api/v1/demo/knowledge?query=PostgreSQL', 200, other)
            assert any(result['source'] == copy['source'] for result in results)
            assert not any(result['source'] == source for result in results)
        self.scenario('same_content_independent_tenant_copy', own_copy)
        investigation = '/api/v1/demo/investigations/' + incident_ids[0]
        self.scenario('tool_allowlist', lambda: self.expect('POST', investigation, 400, analyst, {'tool': 'executeShell'}))

        def tools():
            headers, body = self.expect('POST', investigation, 200, analyst,
                                        {'tool': 'inspectStackTrace', 'repeat': True}, {'X-Correlation-ID': 'portfolio-e2e'})
            assert body['result']['frames']
            assert any(item['cached'] for item in body['audit'])
            assert any(key.lower() == 'x-correlation-id' and value == 'portfolio-e2e' for key, value in headers.items())
        self.scenario('tool_cache_and_correlation', tools)

        def metrics():
            status, _, body = self.request('GET', '/actuator/prometheus', self.token('OPS'))
            assert status == 200 and 'incident_investigator_analysis_requests' in body
            assert 'synthetic-sensitive-value' not in body
        self.scenario('ops_metrics_available', metrics)

    def finish(self):
        self.write_artifacts()
        return int('failed' in self.scenarios.values())

    def write_artifacts(self):
        self.output.mkdir(parents=True, exist_ok=True)
        failed = sum(value == 'failed' for value in self.scenarios.values())
        summary = {'passed': len(self.scenarios) - failed, 'failed': failed, 'scenarios': self.scenarios}
        (self.output/'summary.json').write_text(json.dumps(summary, indent=2) + '\n')
        suite = ET.Element('testsuite', name='portfolio-e2e', tests=str(len(self.scenarios)), failures=str(failed))
        for name, status in self.scenarios.items():
            case = ET.SubElement(suite, 'testcase', name=name)
            if status == 'failed':
                ET.SubElement(case, 'failure', message='Acceptance assertion failed; see scenario and HTTP status evidence')
        ET.ElementTree(suite).write(self.output/'junit.xml', encoding='unicode')
        rows = '\n'.join(f'| {name} | {status} |' for name, status in self.scenarios.items())
        (self.output/'report.md').write_text('# Demo E2E report\n\n| Scenario | Status |\n|---|---|\n' + rows + '\n')
        (self.output/'requests.ndjson').write_text(''.join(json.dumps(item) + '\n' for item in self.requests))


if __name__ == '__main__':
    run_id = time.strftime('%Y%m%d-%H%M%S', time.gmtime()) + '-' + uuid.uuid4().hex[:8]
    runner = Runner(os.environ.get('APP_URL', 'http://app:8080'),
                    Path(os.environ.get('ARTIFACTS_DIR', '/artifacts'))/run_id,
                    Path(os.environ.get('FIXTURES_FILE', '/fixtures/events.ndjson')))
    raise SystemExit(runner.run())
