import importlib.util
import json
import tempfile
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path

SPEC = importlib.util.spec_from_file_location('demo_run', Path(__file__).with_name('run.py'))


class RunnerTest(unittest.TestCase):
    def load_runner(self):
        self.assertTrue(Path(SPEC.origin).is_file(), 'Acceptance runner is missing')
        module = importlib.util.module_from_spec(SPEC)
        SPEC.loader.exec_module(module)
        return module.Runner('http://127.0.0.1:1', Path('/unused'), Path('/unused'))

    def test_failed_scenario_is_recorded_and_does_not_stop_next_scenario(self):
        runner = self.load_runner()
        def fail():
            raise AssertionError('Bearer synthetic-token must not be recorded')
        from contextlib import redirect_stdout
        from io import StringIO
        with redirect_stdout(StringIO()):
            runner.scenario('first', fail)
            runner.scenario('second', lambda: None)
        with tempfile.TemporaryDirectory() as directory:
            runner.output = Path(directory)
            runner.write_artifacts()
            summary = json.loads((runner.output/'summary.json').read_text())
            self.assertEqual(summary['passed'], 1)
            self.assertEqual(summary['failed'], 1)
            xml = ET.parse(runner.output/'junit.xml')
            self.assertEqual(len(xml.findall('testcase')), 2)
            self.assertEqual(len(xml.findall('testcase/failure')), 1)
            for output in runner.output.iterdir():
                self.assertNotIn('synthetic-token', output.read_text())

    def test_http_error_status_is_assertable_without_raising_or_logging_payloads(self):
        from http.server import BaseHTTPRequestHandler, HTTPServer
        from threading import Thread
        class Handler(BaseHTTPRequestHandler):
            def do_GET(self):
                self.send_response(403)
                self.end_headers()
                self.wfile.write(b'{"error":"forbidden"}')
            def log_message(self, *args):
                pass
        runner = self.load_runner()
        with HTTPServer(('127.0.0.1', 0), Handler) as server:
            thread = Thread(target=server.serve_forever, daemon=True)
            thread.start()
            try:
                runner.base = f'http://127.0.0.1:{server.server_port}'
                status, headers, body = runner.request('GET', '/private', token='synthetic-token')
                self.assertEqual(status, 403)
                self.assertEqual(json.loads(body)['error'], 'forbidden')
                self.assertEqual(runner.requests, [{'method': 'GET', 'path': '/private', 'status': 403}])
            finally:
                server.shutdown()
                thread.join()


if __name__ == '__main__':
    unittest.main()
