import { describe, expect, it, vi } from 'vitest';
import { createIncidentApi } from './api';

describe('API session', () => {
  it('deduplicates concurrent token requests and keeps tokens in memory', async () => {
    let tokens = 0;
    const fetcher = vi.fn(async (path: RequestInfo | URL) => {
      if (String(path).startsWith('/local/')) tokens++;
      return new Response(JSON.stringify(String(path).startsWith('/local/') ? { accessToken: 'test-token', expiresAt: new Date(Date.now() + 3600000).toISOString() } : []), { status: 200 });
    });
    const api = createIncidentApi(fetcher);
    await Promise.all([api.list(), api.list()]);
    expect(tokens).toBe(1);
    expect(fetcher.mock.calls.filter(([path]) => path === '/api/v1/incidents')).toHaveLength(2);
  });
  it('turns a rejected analysis into actionable text instead of leaking a response body', async () => {
    const api = createIncidentApi(async path => new Response(JSON.stringify(String(path).startsWith('/local/') ? { accessToken: 'test' } : { detail: 'private information' }), { status: String(path).startsWith('/local/') ? 200 : 503 }));
    await expect(api.analyze('a')).rejects.toThrow('A análise não pôde ser validada');
  });
});
