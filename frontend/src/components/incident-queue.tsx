import { useEffect, useState } from 'react';
import { ArrowDownWideNarrow, ChevronRight, SearchX } from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { Empty, EmptyHeader, EmptyMedia, EmptyTitle, EmptyDescription } from '@/components/ui/empty';
import { severityNames, time, type Incident } from '@/lib/types';
import { cn } from 'cn';
export function IncidentQueue({ incidents, selectedId, busy, loading, clear, select }: { incidents: Incident[]; selectedId: string; busy: boolean; loading: boolean; clear: () => void; select: (id: string) => void }) {
  const [limit, setLimit] = useState(12);
  useEffect(() => setLimit(12), [incidents.length]);
  return <section className="queue panel" aria-label="Fila de incidentes">
    <div className="panel-heading queue-heading"><div><h2>Fila de incidentes <span className="count-pill">{incidents.length}</span></h2><p>Selecione um sinal para investigar.</p></div><ArrowDownWideNarrow aria-label="Mais recentes primeiro" className="muted-icon" /></div>
    <div className="queue-column-head"><span>Incidente / serviço</span><span>Recebido</span></div>
    {loading ? <div className="queue-skeleton" aria-label="Carregando incidentes">{[1, 2, 3].map(item => <Skeleton className="h-20 w-full" key={item} />)}</div> : incidents.length ? <ul className="queue-list" data-large={limit > 48}>{incidents.slice(0, limit).map(item => <li key={item.id}><button className={cn('queue-row', selectedId === item.id && 'is-selected')} type="button" disabled={busy} onClick={() => select(item.id)} aria-label={`Selecionar incidente ${item.title}`} aria-pressed={selectedId === item.id}>
      <span className={cn('severity-marker', `severity-${item.severity.toLowerCase()}`)} aria-hidden="true" /><span className="queue-row-copy"><strong>{item.title.replace(`${item.source}: `, '')}</strong><span><code translate="no">{item.source}</code><Badge variant="outline" className={cn('severity-badge', `severity-${item.severity.toLowerCase()}`)}>{severityNames[item.severity] ?? item.severity}</Badge></span></span><span className="queue-row-time"><time dateTime={item.createdAt}>{time(item.createdAt).slice(0, 5)}</time><ChevronRight aria-hidden="true" /></span>
    </button></li>)}</ul> : <Empty><EmptyHeader><EmptyMedia variant="icon"><SearchX aria-hidden="true" /></EmptyMedia><EmptyTitle>Nenhum incidente encontrado</EmptyTitle><EmptyDescription>Altere a busca, o serviço ou a janela de observação.</EmptyDescription></EmptyHeader><Button variant="outline" onClick={clear}>Limpar filtros</Button></Empty>}
    {incidents.length > limit ? <Button variant="ghost" className="m-3" onClick={() => setLimit(value => value + 12)}>Ver mais incidentes</Button> : null}
    <div className="queue-footer"><span>Mais recentes primeiro</span><span>{Math.min(limit, incidents.length)} de {incidents.length}</span></div>
  </section>;
}
