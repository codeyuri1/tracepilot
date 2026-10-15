"""Exercise the fixture CLI as used by contributors, without external dependencies."""
import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

SCRIPT = Path(__file__).with_name('generate-test-data.py')


class FixtureGeneratorTest(unittest.TestCase):
    def generate(self, path, count='30', seed='42'):
        return subprocess.run([sys.executable, str(SCRIPT), '--count', count,
                               '--seed', seed, '--output', str(path)], capture_output=True)

    def test_generates_reproducible_api_payloads_covering_five_scenarios_and_three_severities(self):
        with tempfile.TemporaryDirectory() as directory:
            first, second = Path(directory)/'first.ndjson', Path(directory)/'second.ndjson'
            self.assertEqual(self.generate(first).returncode, 0)
            self.assertEqual(self.generate(second).returncode, 0)
            self.assertEqual(first.read_bytes(), second.read_bytes())
            rows = [json.loads(line) for line in first.read_text().splitlines()]
            self.assertEqual(len(rows), 30)
            self.assertEqual({r['service'] for r in rows},
                             {'payment-worker', 'checkout-api', 'auth-api', 'orders-worker'})
            self.assertEqual({r['level'] for r in rows}, {'INFO', 'WARN', 'ERROR'})
            self.assertTrue(any('fallback completed' in r['stackTrace'] and r['level'] == 'INFO' for r in rows))
            self.assertTrue(all(set(r) == {'service', 'level', 'message', 'stackTrace'} for r in rows))
            self.assertTrue(all(20 <= len(r['stackTrace']) <= 50000 for r in rows))
            self.assertNotIn('password', first.read_text().lower())
            self.assertNotIn('bearer', first.read_text().lower())
            third = Path(directory)/'third.ndjson'
            self.assertEqual(self.generate(third, seed='43').returncode, 0)
            self.assertNotEqual(first.read_bytes(), third.read_bytes())

    def test_rejects_invalid_sizes_without_writing_output(self):
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory)/'events.ndjson'
            for count in ['0', '-1', '10001', 'bad']:
                with self.subTest(count=count):
                    self.assertNotEqual(self.generate(output, count=count).returncode, 0)
                    self.assertFalse(output.exists())

    def test_refuses_to_overwrite_existing_fixture(self):
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory)/'events.ndjson'
            output.write_text('keep me')
            self.assertNotEqual(self.generate(output).returncode, 0)
            self.assertEqual(output.read_text(), 'keep me')


if __name__ == '__main__':
    unittest.main()
