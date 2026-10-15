import { useEffect, useRef, useState } from 'react';
import { Activity, ArrowUpRight, BookOpen, ChevronDown, CircleHelp, Code2, Fingerprint, LayoutDashboard, ListFilter, Moon, Network, Pause, Play, Radio, RefreshCw, Search, ShieldCheck, Sun, X, AlertCircle, Menu } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '@/components/ui/tooltip';
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle, DialogTrigger } from '@/components/ui/dialog';
import { ServiceMap } from '@/components/service-map';
import { ActivityChart } from '@/components/activity-chart';
import { IncidentQueue } from '@/components/incident-queue';
import { InvestigationPanel } from '@/components/investigation-panel';
import { ScenarioDialog } from '@/components/scenario-dialog';
import { useInvestigation } from '@/hooks/use-investigation';
import { time } from '@/lib/types';
import { cn } from 'cn';

function Guide() {
  return <Dialog><DialogTrigger asChild><button type="button" className="nav-item"><CircleHelp aria-hidden="true" />Guia da demonstração<ArrowUpRight aria-hidden="true" /></button></DialogTrigger><DialogContent className="guide-dialog"><DialogHeader><DialogTitle>Explore a investigação</DialogTitle><DialogDescription>Um ambiente local com dados sintéticos. O diagnóstico pode usar um modelo de demonstração ou uma LLM pela API compatível com OpenAI. Confira o modelo identificado na análise.</DialogDescription></DialogHeader><ol className="guide-steps"><li><span>1</span><div><strong>Escolha um sinal</strong><p>Selecione na fila ou filtre por serviço no mapa.</p></div></li><li><span>2</span><div><strong>Investigue com contexto</strong><p>Confira a causa provável, os trechos verificados e as recomendações.</p></div></li><li><span>3</span><div><strong>Teste outros cenários</strong><p>Use Novo incidente para simular banco, timeout, identidade, worker ou cache recuperado.</p></div></li></ol><div className="guide-note"><ShieldCheck aria-hidden="true" /><p>A topologia é ilustrativa. Contagens e atividade vêm da API. A confiança da demo não é uma probabilidade calibrada.</p></div><Button asChild variant="outline"><a href="http://localhost:3000" target="_blank" rel="noreferrer">Abrir Grafana<ArrowUpRight aria-hidden="true" data-icon="inline-end" /></a></Button><p className="muted-note">Grafana requer o profile opcional de observabilidade; não é iniciado pela showcase padrão.</p></DialogContent></Dialog>;
}
function Architecture() {
  return <Dialog><DialogTrigger asChild><button type="button" className="nav-item"><Network aria-hidden="true" />Arquitetura</button></DialogTrigger><DialogContent className="guide-dialog"><DialogHeader><DialogTitle>O caminho de um incidente</DialogTitle><DialogDescription>O produtor usa a mesma API da interface. Cada etapa mantém seu contrato explícito.</DialogDescription></DialogHeader><div className="architecture-flow">{['Evento sintético', 'API e sanitização', 'PostgreSQL', 'Modelo de análise', 'Validação de evidências'].map((label, index) => <div key={label}><span>{index + 1}</span><strong>{label}</strong></div>)}</div><p className="muted-note">RAG e tools têm endpoints auxiliares de demonstração. A análise padrão usa o material do incidente. Kafka e processamento assíncrono são evoluções planejadas.</p></DialogContent></Dialog>;
}
export default function App() {
  const investigation = useInvestigation();
  const { incidents, loading, paused, setPaused, busy, refreshing, updatedAt, error, message } = investigation;
  const [query, setQuery] = useState(() => new URLSearchParams(location.search).get('q') ?? '');
  const [severity, setSeverity] = useState(() => new URLSearchParams(location.search).get('severity') ?? 'ALL');
  const [service, setService] = useState(() => new URLSearchParams(location.search).get('service') ?? '');
  const [period, setPeriod] = useState(() => { const value = new URLSearchParams(location.search).get('period'); return ['15', '60', '1440'].includes(value ?? '') ? value! : '60'; });
  const [theme, setTheme] = useState(() => { try { return localStorage.getItem('investigator-theme-v1') === 'light' ? 'light' : 'dark'; } catch { return 'dark'; } });
  const [menuOpen, setMenuOpen] = useState(false);
  const search = useRef<HTMLInputElement>(null);
  useEffect(() => {
    document.documentElement.classList.remove('light', 'dark'); document.documentElement.classList.add(theme);
    document.querySelector('meta[name="theme-color"]')?.setAttribute('content', theme === 'dark' ? '#101521' : '#f5f6fb');
    try { localStorage.setItem('investigator-theme-v1', theme); } catch { /* Preference is optional. */ }
  }, [theme]);
  useEffect(() => {
    function shortcut(event: KeyboardEvent) {
      const target = event.target as HTMLElement;
      if (event.key === '/' && !event.ctrlKey && !event.metaKey && !event.altKey && !['INPUT', 'TEXTAREA', 'SELECT'].includes(target.tagName) && !target.isContentEditable && !document.querySelector('[role="dialog"]')) { event.preventDefault(); search.current?.focus(); }
    }
    document.addEventListener('keydown', shortcut); return () => document.removeEventListener('keydown', shortcut);
  }, []);
  useEffect(() => {
    const url = new URL(location.href);
    for (const [key, value] of Object.entries({ q: query, severity: severity === 'ALL' ? '' : severity, service, period })) { if (value) url.searchParams.set(key, value); else url.searchParams.delete(key); }
    history.replaceState(null, '', url);
  }, [query, severity, service, period]);
  const now = updatedAt ?? Date.now();
  const windowIncidents = incidents.filter(item => { const age = now - new Date(item.createdAt).getTime(); return age >= 0 && age <= Number(period) * 60000; });
  const filtered = windowIncidents.filter(item => (severity === 'ALL' || item.severity === severity) && (!service || item.source === service) && `${item.title} ${item.source} ${item.id}`.toLocaleLowerCase('pt-BR').includes(query.toLocaleLowerCase('pt-BR')));
  const high = windowIncidents.filter(item => ['HIGH', 'CRITICAL'].includes(item.severity)).length;
  const services = new Set(windowIncidents.map(item => item.source)).size;
  const hasFilters = Boolean(query || service || severity !== 'ALL');
  function clear() { setQuery(''); setSeverity('ALL'); setService(''); setPeriod('1440'); }
  return <TooltipProvider delayDuration={250}><a href="#main-content" className="skip-link">Pular para o conteúdo</a><div className="app-shell">
    <aside className={cn('sidebar', menuOpen && 'mobile-open')}><a className="brand" href="#overview" aria-label="TracePilot — início"><span className="brand-mark"><Fingerprint aria-hidden="true" /></span><span>trace<span>pilot<span className="brand-period">.</span></span></span></a>
      <div className="workspace"><span className="workspace-avatar">D</span><span><strong>Workspace demo</strong><small>Ambiente local</small></span><ChevronDown aria-hidden="true" /></div>
      <div className="nav-section-label">Workspace</div><nav aria-label="Navegação principal"><a href="#overview" className="nav-item is-active" onClick={() => setMenuOpen(false)}><LayoutDashboard aria-hidden="true" />Visão geral</a><a href="#investigations" className="nav-item" onClick={() => setMenuOpen(false)}><Activity aria-hidden="true" />Investigações<span className="nav-count">{incidents.length}</span></a><Architecture /></nav>
      <div className="nav-section-label resource-label">Recursos</div><nav aria-label="Recursos"><a href="/swagger-ui/index.html" className="nav-item" target="_blank" rel="noreferrer"><Code2 aria-hidden="true" />API & contratos<ArrowUpRight aria-hidden="true" /></a><Guide /></nav>
      <div className="sidebar-bottom"><div className="demo-disclosure"><span className="demo-icon"><ShieldCheck aria-hidden="true" /></span><strong>Feito para explorar.</strong><p>Dados sintéticos.<br />Evidências verificáveis.</p><Badge variant="outline">Ambiente de testes</Badge></div><div className="sidebar-identity"><span>AI</span><div><strong>TracePilot</strong><small>Java 21 / Spring AI</small></div></div></div>
    </aside>
    <div className="workspace-shell"><header className="topbar"><Button variant="ghost" size="icon" className="mobile-menu" aria-label={menuOpen ? 'Fechar navegação' : 'Abrir navegação'} aria-expanded={menuOpen} onClick={() => setMenuOpen(value => !value)}><Menu aria-hidden="true" /></Button><div className="global-search"><Search aria-hidden="true" /><Input type="search" name="incident-search" autoComplete="off" placeholder="Buscar incidente, serviço ou ID…" aria-label="Buscar incidentes" ref={search} value={query} onChange={event => setQuery(event.target.value)} /><kbd aria-hidden="true">/</kbd></div><div className="topbar-actions"><span className="environment-indicator"><span />Ambiente local</span><Tooltip><TooltipTrigger asChild><Button variant="ghost" size="icon" aria-label={theme === 'dark' ? 'Ativar tema claro' : 'Ativar tema escuro'} onClick={() => setTheme(theme === 'dark' ? 'light' : 'dark')}>{theme === 'dark' ? <Sun aria-hidden="true" /> : <Moon aria-hidden="true" />}</Button></TooltipTrigger><TooltipContent>Alternar tema</TooltipContent></Tooltip><span className="topbar-divider" /><ScenarioDialog error={error} busy={busy} onIngest={async scenario => { const created = await investigation.ingest(scenario); if (created) { setQuery(''); setService(''); setSeverity('ALL'); } return created; }} /></div></header>
      <main id="main-content" tabIndex={-1}><section className="page-intro" id="overview"><div><div className="breadcrumb">Workspace<span>/</span>Visão geral</div><h1>Central de investigação</h1><p>Conecte os sinais. Entenda a causa. Saiba onde agir.</p></div><div className="live-controls"><span className={cn('live-badge', paused && 'is-paused')}><Radio aria-hidden="true" />{paused ? 'Atualização pausada' : 'Acompanhamento ao vivo'}</span><Tooltip><TooltipTrigger asChild><Button variant="outline" size="icon" aria-label={paused ? 'Retomar atualização' : 'Pausar atualização'} onClick={() => setPaused(value => !value)}>{paused ? <Play aria-hidden="true" /> : <Pause aria-hidden="true" />}</Button></TooltipTrigger><TooltipContent>{paused ? 'Retomar atualização a cada 5 segundos' : 'Pausar atualização da interface'}</TooltipContent></Tooltip><Tooltip><TooltipTrigger asChild><Button variant="outline" size="icon" disabled={refreshing || busy} aria-label="Atualizar agora" onClick={() => void investigation.refresh()}><RefreshCw aria-hidden="true" className={cn(refreshing && 'spin')} /></Button></TooltipTrigger><TooltipContent>Atualizar agora</TooltipContent></Tooltip></div></section>
        {error ? <Alert variant="destructive" className="error-alert"><AlertCircle aria-hidden="true" /><AlertTitle>Não foi possível concluir a solicitação</AlertTitle><AlertDescription>{error}<Button variant="outline" disabled={refreshing || busy} onClick={investigation.retry}>Tentar novamente</Button></AlertDescription></Alert> : null}
        <div className="summary-strip"><div><span className="summary-icon"><Activity aria-hidden="true" /></span><span>Incidentes recebidos<strong>{windowIncidents.length}<small>na janela</small></strong></span></div><div><span className="summary-icon priority-icon"><ListFilter aria-hidden="true" /></span><span>Prioridade alta<strong>{high}<small>alto ou crítico</small></strong></span></div><div><span className="summary-icon services-icon"><Network aria-hidden="true" /></span><span>Serviços com sinais<strong>{services}<small>origens distintas</small></strong></span></div><div><span className="summary-icon"><RefreshCw aria-hidden="true" /></span><span>Última sincronização<strong className="sync-time">{updatedAt ? time(updatedAt) : '—'}<small>{paused ? 'Interface pausada' : 'A cada 5 segundos'}</small></strong></span></div></div>
        <div className="signals-layout"><ServiceMap incidents={windowIncidents} service={service} onSelect={setService} /><ActivityChart incidents={windowIncidents} period={period} onPeriod={setPeriod} now={now} /></div>
        <section id="investigations" className="investigations-section"><div className="section-heading"><div><h2>Do sinal à investigação</h2><p>Uma fila organizada. Um diagnóstico com contexto.</p></div><div className="queue-filters"><label htmlFor="severity-filter"><ListFilter aria-hidden="true" /><span className="sr-only">Severidade</span></label><select id="severity-filter" name="severity" value={severity} onChange={event => setSeverity(event.target.value)}><option value="ALL">Todas as severidades</option><option value="CRITICAL">Crítico</option><option value="HIGH">Alto</option><option value="MEDIUM">Médio</option><option value="LOW">Leve</option></select>{hasFilters ? <Button variant="ghost" onClick={clear} aria-label="Limpar filtros"><X aria-hidden="true" data-icon="inline-start" />Limpar filtros</Button> : null}</div></div>
          {service ? <div className="filter-context">Filtrando serviço <code translate="no">{service}</code><button type="button" aria-label="Remover filtro de serviço" onClick={() => setService('')}><X aria-hidden="true" /></button></div> : null}
          <div className="investigations-layout"><IncidentQueue incidents={filtered} selectedId={investigation.selectedId} busy={busy} loading={loading} clear={clear} select={investigation.select} /><InvestigationPanel key={investigation.selectedId} incident={investigation.incident} analysis={investigation.analysis} busy={busy} analyze={() => void investigation.analyze()} /></div>
        </section>
        <p role="status" className="action-status">{message}</p><footer className="workspace-footer"><span><ShieldCheck aria-hidden="true" />Dados sintéticos. A análise precisa de evidências.</span><span>TracePilot <span className="footer-dot">/</span> Portfolio edition</span></footer>
      </main>
    </div>
  </div></TooltipProvider>;
}
