#!/usr/bin/env python3
"""Create deterministic, synthetic log-event payloads. No production data is read."""
import argparse
import json
import random
from pathlib import Path

SCENARIOS = (
    ('payment-worker', 'ERROR', 'Falha ao conectar no PostgreSQL',
     'org.postgresql.util.PSQLException: Connection refused'),
    ('checkout-api', 'WARN', 'Tempo limite na autorização de pagamento',
     'java.net.SocketTimeoutException: Read timed out'),
    ('auth-api', 'ERROR', 'Dependência de identidade indisponível',
     'HTTP 503 Service Unavailable from identity-provider'),
    ('orders-worker', 'ERROR', 'Worker não conseguiu processar o pedido',
     'java.lang.IllegalStateException: order state is invalid'),
    ('orders-worker', 'INFO', 'Cache ausente; consulta recuperada pelo fallback',
     'demo.cache.CacheMiss: cache miss; fallback completed'),
)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--count', type=int, default=30)
    parser.add_argument('--seed', type=int, default=42)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    if not 1 <= args.count <= 10000:
        parser.error('--count must be between 1 and 10000')
    rng = random.Random(args.seed)
    rows = []
    for index in range(args.count):
        service, level, message, stack = SCENARIOS[index % len(SCENARIOS)]
        rows.append(json.dumps(dict(service=service, level=level, message=message,
                                    stackTrace=f'{stack}\n    at demo.synthetic.Worker.run(Worker.java:{rng.randint(10, 999)})'),
                               ensure_ascii=False))
    args.output.parent.mkdir(parents=True, exist_ok=True)
    try:
        with args.output.open('x', encoding='utf-8') as output:
            output.write('\n'.join(rows) + '\n')
    except FileExistsError:
        parser.error('output already exists; choose a new path')
    print(f'Generated {args.count} synthetic events: {args.output}')


if __name__ == '__main__':
    main()
