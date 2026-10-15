import { act, cleanup, fireEvent, render, screen, within } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import App from './App';

const incidents = [
  { id: 'a', title: 'Pagamento indisponível', source: 'payment-worker', severity: 'HIGH', status: 'RECEIVED', createdAt: new Date().toISOString() },
  { id: 'b', title: 'Checkout lento', source: 'checkout-api', severity: 'MEDIUM', status: 'RECEIVED', createdAt: new Date().toISOString() },
];
const analysis = { category: 'DATABASE_CONNECTIVITY', summary: 'O PostgreSQL recusou a conexão.', probableCause: 'Banco indisponível', confidence: 0.9, evidence: [{ type: 'STACK_TRACE', reference: 'incident.stackTrace', excerpt: 'Connection refused' }], recommendations: ['Verifique a disponibilidade do PostgreSQL.'], model: 'deterministic-demo-model', promptVersion: 'v1' };
function mockApi(options: { offline?: boolean; history?: () => unknown[]; pending?: boolean } = {}) {
  const mock = vi.fn(async (path: string, init?: RequestInit) => {
    if (options.offline) throw new TypeError('Failed to fetch');
    let body: unknown = incidents.map(item => ({ ...item }));
    if (path.startsWith('/local/')) body = { accessToken: 'synthetic-token', expiresAt: new Date(Date.now() + 3600000).toISOString() };
    else if (path.endsWith('/analyses')) {
      if (init?.method === 'POST' && options.pending) return new Promise(() => {});
      body = init?.method === 'POST' ? analysis : options.history ? options.history() : [analysis];
    } else if (init?.method === 'POST') body = { ...incidents[1], id: 'created' };
    return { ok: true, status: 200, json: async () => body };
  });
  vi.stubGlobal('fetch', mock);
  return mock;
}
beforeEach(() => { localStorage.clear(); window.history.replaceState(null, '', '/'); });
afterEach(() => { cleanup(); vi.unstubAllGlobals(); vi.restoreAllMocks(); });

describe('investigation workspace', () => {
  it('loads real incidents and the selected diagnosis automatically', async () => {
    mockApi(); render(<App />);
    expect(screen.getByRole('heading', { name: 'Central de investigação' })).toBeInTheDocument();
    expect(await screen.findByText('O PostgreSQL recusou a conexão.')).toBeInTheDocument();
    expect(screen.getByText('Dados sintéticos. A análise precisa de evidências.')).toBeInTheDocument();
  });
  it('filters the queue by a search query and clears it', async () => {
    mockApi(); render(<App />);
    await screen.findByRole('button', { name: /Selecionar incidente Checkout lento/ });
    fireEvent.change(screen.getByRole('searchbox', { name: 'Buscar incidentes' }), { target: { value: 'checkout' } });
    const queue = within(screen.getByRole('region', { name: 'Fila de incidentes' }));
    expect(queue.queryByRole('button', { name: /Selecionar incidente Pagamento/ })).not.toBeInTheDocument();
    expect(queue.getByRole('button', { name: /Selecionar incidente Checkout/ })).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Limpar filtros' }));
    expect(queue.getByRole('button', { name: /Selecionar incidente Pagamento/ })).toBeInTheDocument();
  });
  it('filters incidents by severity', async () => {
    mockApi(); render(<App />);
    await screen.findByRole('button', { name: /Selecionar incidente Checkout/ });
    fireEvent.change(screen.getByLabelText('Severidade'), { target: { value: 'MEDIUM' } });
    expect(screen.queryByRole('button', { name: /Selecionar incidente Pagamento/ })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Selecionar incidente Checkout/ })).toBeInTheDocument();
  });
  it('filters by a service selected in the map', async () => {
    mockApi(); render(<App />);
    await screen.findByRole('button', { name: /Selecionar incidente Checkout/ });
    fireEvent.click(screen.getByRole('button', { name: 'Filtrar serviço checkout-api' }));
    expect(screen.queryByRole('button', { name: /Selecionar incidente Pagamento/ })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Selecionar incidente Checkout/ })).toBeInTheDocument();
  });
  it('presents an actionable empty search state', async () => {
    mockApi(); render(<App />);
    await screen.findByRole('button', { name: /Selecionar incidente Checkout/ });
    fireEvent.change(screen.getByRole('searchbox', { name: 'Buscar incidentes' }), { target: { value: 'nothing-here' } });
    expect(screen.getByText('Nenhum incidente encontrado')).toBeInTheDocument();
    expect(within(screen.getByRole('region', { name: 'Fila de incidentes' })).getByRole('button', { name: 'Limpar filtros' })).toBeInTheDocument();
  });
  it('pauses polling without clearing the current diagnosis', async () => {
    mockApi(); const timer = vi.spyOn(window, 'clearInterval'); render(<App />);
    await screen.findByText('O PostgreSQL recusou a conexão.');
    fireEvent.click(screen.getByRole('button', { name: 'Pausar atualização' }));
    expect(screen.getByRole('button', { name: 'Retomar atualização' })).toBeInTheDocument();
    expect(screen.getByText('O PostgreSQL recusou a conexão.')).toBeInTheDocument();
    expect(timer).toHaveBeenCalled();
  });
  it('creates the chosen synthetic scenario through the API', async () => {
    const fetcher = mockApi(); render(<App />);
    fireEvent.click(screen.getByRole('button', { name: 'Novo incidente' }));
    const dialog = within(screen.getByRole('dialog', { name: 'Simular um incidente' }));
    fireEvent.click(dialog.getByRole('radio', { name: /Timeout de dependência/ }));
    fireEvent.click(dialog.getByRole('button', { name: 'Injetar incidente' }));
    await screen.findByText('Incidente criado. Selecione Analisar incidente para investigar.');
    const request = fetcher.mock.calls.find(([path, init]) => path === '/api/v1/demo/log-events' && init?.method === 'POST');
    expect(JSON.parse(request?.[1]?.body as string)).toMatchObject({ service: 'checkout-api', stackTrace: 'java.net.SocketTimeoutException: Read timed out' });
  });
  it('reports offline state with a retry action', async () => {
    mockApi({ offline: true }); render(<App />);
    expect(await screen.findByRole('alert')).toHaveTextContent(/Não foi possível conectar/);
    expect(screen.getByRole('button', { name: 'Tentar novamente' })).toBeInTheDocument();
  });
  it('persists theme preference without storing credentials', () => {
    mockApi(); render(<App />);
    fireEvent.click(screen.getByRole('button', { name: 'Ativar tema claro' }));
    expect(document.documentElement).toHaveClass('light');
    expect(localStorage.getItem('investigator-theme-v1')).toBe('light');
    expect(JSON.stringify(localStorage)).not.toContain('synthetic-token');
  });
  it('refreshes history after a simulator analysis completes', async () => {
    let ready = false; mockApi({ history: () => ready ? [analysis] : [] });
    const intervals = vi.spyOn(window, 'setInterval'); render(<App />);
    await screen.findByText('Pronto para investigar');
    ready = true;
    const poll = intervals.mock.calls.find(([, delay]) => delay === 5000)?.[0] as () => void;
    await act(async () => { poll(); });
    expect(await screen.findByText('O PostgreSQL recusou a conexão.')).toBeInTheDocument();
  });
  it('keeps selection stable during a pending analysis', async () => {
    mockApi({ pending: true }); render(<App />);
    await screen.findByText('O PostgreSQL recusou a conexão.');
    fireEvent.click(screen.getByRole('button', { name: 'Analisar incidente' }));
    expect(screen.getByRole('button', { name: /Selecionar incidente Checkout/ })).toBeDisabled();
  });
  it('keeps a newly created incident when an older refresh finishes late', async () => {
    const base = mockApi();
    let lists = 0;
    let release: ((value: unknown) => void) | undefined;
    vi.stubGlobal('fetch', async (path: string, init?: RequestInit) => {
      if (path === '/api/v1/incidents' && !init?.method && ++lists === 2) return new Promise(resolve => { release = resolve; });
      if (path === '/api/v1/demo/log-events') return { ok: true, status: 201, json: async () => ({ ...incidents[0], id: 'new-incident', title: 'Criado agora', createdAt: new Date().toISOString() }) };
      return base(path, init);
    });
    render(<App />);
    await screen.findByText('O PostgreSQL recusou a conexão.');
    fireEvent.click(screen.getByRole('button', { name: 'Atualizar agora' }));
    fireEvent.click(screen.getByRole('button', { name: 'Novo incidente' }));
    fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Injetar incidente' }));
    await screen.findByRole('heading', { name: 'Criado agora' });
    await act(async () => { release?.({ ok: true, status: 200, json: async () => incidents }); });
    expect(screen.getByRole('heading', { name: 'Criado agora' })).toBeInTheDocument();
  });
  it('presents a failed ingestion inside the open dialog with a retry action', async () => {
    const base = mockApi();
    vi.stubGlobal('fetch', async (path: string, init?: RequestInit) => {
      if (path === '/api/v1/demo/log-events') return { ok: false, status: 500, json: async () => ({ detail: 'private error' }) };
      return base(path, init);
    });
    render(<App />);
    fireEvent.click(screen.getByRole('button', { name: 'Novo incidente' }));
    const dialog = within(screen.getByRole('dialog'));
    fireEvent.click(dialog.getByRole('button', { name: 'Injetar incidente' }));
    expect(await dialog.findByRole('alert')).toHaveTextContent(/tente novamente/i);
    expect(dialog.getByRole('button', { name: 'Injetar incidente' })).toBeEnabled();
  });

  it('offers Leve, Médio and Alto incidents with a recovered low-severity scenario', () => {
    mockApi(); render(<App />);
    expect(screen.getByRole('option', { name: 'Leve' })).toHaveValue('LOW');
    expect(screen.getByRole('option', { name: 'Médio' })).toHaveValue('MEDIUM');
    expect(screen.getByRole('option', { name: 'Alto' })).toHaveValue('HIGH');
    fireEvent.click(screen.getByRole('button', { name: 'Novo incidente' }));
    const dialog = within(screen.getByRole('dialog'));
    expect(dialog.getByRole('radio', { name: /Cache recomposto/ })).toBeInTheDocument();
    expect(dialog.getByText('Leve', { exact: true })).toBeInTheDocument();
  });

  it('identifies a real model instead of labelling its analysis as deterministic', async () => {
    mockApi({ history: () => [{ ...analysis, model: 'qwen2.5-coder:7b' }] }); render(<App />);
    expect(await screen.findByText('qwen2.5-coder:7b', { exact: true })).toBeInTheDocument();
    expect(screen.getByText('LLM real', { exact: true })).toBeInTheDocument();
  });

});
