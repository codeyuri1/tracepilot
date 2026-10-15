import { ChartNoAxesColumnIncreasing } from 'lucide-react';
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group';
import type { Incident } from '@/lib/types';
import { time } from '@/lib/types';
export function ActivityChart({ incidents, period, onPeriod, now }: { incidents: Incident[]; period: string; onPeriod: (value: string) => void; now: number }) {
  const minutes = Number(period);
  const bucket = minutes * 60000 / 24;
  const values = Array.from({ length: 24 }, (_, index) => incidents.filter(item => {
    const ago = now - new Date(item.createdAt).getTime();
    return ago >= bucket * (23 - index) && ago < bucket * (24 - index);
  }).length);
  const maximum = Math.max(...values, 1);
  return <section className="activity-panel panel" aria-labelledby="activity-title">
    <div className="panel-heading"><div><h2 id="activity-title"><ChartNoAxesColumnIncreasing aria-hidden="true" /> Atividade de ingestão</h2><p>Eventos recebidos pela API.</p></div></div>
    <div className="activity-meta"><strong>{incidents.length}<span>incidentes na janela</span></strong><ToggleGroup type="single" value={period} onValueChange={value => value && onPeriod(value)} aria-label="Janela de observação" size="sm" spacing={0}>
      <ToggleGroupItem value="15" aria-label="Últimos 15 minutos">15 min</ToggleGroupItem><ToggleGroupItem value="60" aria-label="Última hora">1 h</ToggleGroupItem><ToggleGroupItem value="1440" aria-label="Últimas 24 horas">24 h</ToggleGroupItem>
    </ToggleGroup></div>
    <div className="activity-bars" role="img" aria-label={`${incidents.length} incidentes nos últimos ${minutes} minutos, em 24 intervalos. Maior intervalo: ${maximum === 1 && !values.some(Boolean) ? 0 : maximum} incidentes.`}>
      {values.map((value, index) => <div className="bar-track" key={index}><div className="activity-bar" style={{ height: `${Math.max(value / maximum * 100, 3)}%` }} data-empty={!value} title={`${time(now - bucket * (24 - index))}: ${value} incidentes`} /></div>)}
    </div>
    <div className="axis-labels"><span>{time(now - minutes * 60000).slice(0, 5)}</span><span>Agora</span></div>
    <div className="activity-footnote"><span className="legend-square" /> Volume real de incidentes<span>Sem dados de latência</span></div>
  </section>;
}
