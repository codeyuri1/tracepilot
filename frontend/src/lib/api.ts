import type { Analysis, Incident, Scenario } from './types';
type Role = 'INCIDENT_READER' | 'INCIDENT_WRITER' | 'ANALYST';

/** Short-lived local demo sessions, scoped to this client and never persisted in storage. */
export function createIncidentApi(fetcher: typeof fetch = (...args) => fetch(...args)) {
  const sessions = new Map<Role, { expires: number; value: Promise<string> }>();
  async function token(role: Role) {
    const cached = sessions.get(role);
    if (cached && cached.expires > Date.now()) return cached.value;
    const entry = { expires: Date.now() + 60000, value: Promise.resolve('') };
    entry.value = fetcher(`/local/demo-token?role=${role}&tenant=demo`)
      .then(async response => {
        if (!response.ok) throw new Error('A sessão de demonstração não está disponível. Confira o profile local da API.');
        const body = await response.json();
        if (!body.accessToken) throw new Error('A sessão de demonstração não pôde ser iniciada. Tente novamente.');
        if (body.expiresAt) entry.expires = new Date(body.expiresAt).getTime() - 60000;
        return body.accessToken as string;
      }).catch(error => { sessions.delete(role); throw error; });
    sessions.set(role, entry);
    return entry.value;
  }
  async function request<T>(path: string, role: Role, init: RequestInit = {}): Promise<T> {
    try {
      const accessToken = await token(role);
      const response = await fetcher(path, { ...init, headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${accessToken}` } });
      if (!response.ok) {
        if (response.status === 401) sessions.delete(role);
        if (response.status === 503) throw new Error('A análise não pôde ser validada. Tente novamente ou escolha outro cenário.');
        throw new Error(`A API respondeu ${response.status}. Atualize os dados e tente novamente.`);
      }
      return await response.json() as T;
    } catch (error) {
      if (error instanceof TypeError) throw new Error('Não foi possível conectar à API. Verifique os containers e tente novamente.');
      throw error;
    }
  }
  return {
    list: (signal?: AbortSignal) => request<Incident[]>('/api/v1/incidents', 'INCIDENT_READER', { signal }),
    history: (id: string, signal?: AbortSignal) => request<Analysis[]>(`/api/v1/incidents/${encodeURIComponent(id)}/analyses`, 'INCIDENT_READER', { signal }),
    analyze: (id: string) => request<Analysis>(`/api/v1/incidents/${encodeURIComponent(id)}/analyses`, 'ANALYST', { method: 'POST' }),
    ingest: (scenario: Scenario) => request<Incident>('/api/v1/demo/log-events', 'INCIDENT_WRITER', { method: 'POST', body: JSON.stringify({ service: scenario.service, level: scenario.level, message: scenario.message, stackTrace: scenario.stackTrace }) }),
  };
}
