export type Incident = { id: string; title: string; source: string; severity: string; status: string; createdAt: string };
export type Evidence = { type: string; reference: string; excerpt: string };
export type Analysis = { id?: string; category: string; summary: string; probableCause: string; confidence: number; evidence: Evidence[]; recommendations: string[]; model: string; promptVersion: string };
export type Scenario = { id: string; name: string; description: string; service: string; level: string; message: string; stackTrace: string };
export const scenarios: Scenario[] = [
  { id: 'database', name: 'Conexão recusada com o banco', description: 'Conexão recusada pelo PostgreSQL', service: 'payment-worker', level: 'ERROR', message: 'Falha ao conectar no PostgreSQL', stackTrace: 'org.postgresql.util.PSQLException: Connection refused' },
  { id: 'timeout', name: 'Timeout de dependência', description: 'Tempo limite na autorização de pagamento', service: 'checkout-api', level: 'WARN', message: 'Tempo limite na autorização de pagamento', stackTrace: 'java.net.SocketTimeoutException: Read timed out' },
  { id: 'identity', name: 'Serviço indisponível', description: 'Provedor de identidade responde HTTP 503', service: 'auth-api', level: 'ERROR', message: 'Dependência de identidade indisponível', stackTrace: 'HTTP 503 Service Unavailable from identity-provider' },
  { id: 'worker', name: 'Estado de pedido inválido', description: 'Transição de estado interrompe o worker', service: 'orders-worker', level: 'ERROR', message: 'Worker não conseguiu processar o pedido', stackTrace: 'java.lang.IllegalStateException: order state is invalid' },
  { id: 'cache', name: 'Cache recomposto', description: 'Cache miss recuperado pelo fallback, sem falha definitiva', service: 'orders-worker', level: 'INFO', message: 'Cache ausente; consulta recuperada pelo fallback', stackTrace: 'demo.cache.CacheMiss: cache miss; fallback completed' },
];
export const severityNames: Record<string, string> = { CRITICAL: 'Crítico', HIGH: 'Alto', MEDIUM: 'Médio', LOW: 'Leve' };
export const categoryNames: Record<string, string> = { DATABASE_CONNECTIVITY: 'Conectividade com o banco', DEPENDENCY_TIMEOUT: 'Tempo limite excedido', DEPENDENCY_UNAVAILABLE: 'Dependência indisponível', INVALID_STATE: 'Estado de pedido inválido', RECOVERED_CACHE_MISS: 'Cache recuperado por fallback' };
const clock = new Intl.DateTimeFormat('pt-BR', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
export function time(value: string | number) { return clock.format(new Date(value)); }

export function scenarioSeverity(level: string) { return level === 'INFO' ? 'LOW' : level === 'WARN' ? 'MEDIUM' : 'HIGH'; }
