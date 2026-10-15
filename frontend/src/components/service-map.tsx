import { Database, Fingerprint, Layers3, ShieldCheck, Timer, Workflow } from 'lucide-react';
import type { Incident } from '@/lib/types';
import { cn } from 'cn';
const services = [
  { name: 'payment-worker', label: 'Pagamentos', Icon: Database },
  { name: 'checkout-api', label: 'Checkout', Icon: Timer },
  { name: 'auth-api', label: 'Identidade', Icon: ShieldCheck },
  { name: 'orders-worker', label: 'Pedidos', Icon: Layers3 },
];
export function ServiceMap({ incidents, service, onSelect }: { incidents: Incident[]; service: string; onSelect: (value: string) => void }) {
  return <section className="service-map panel" aria-labelledby="service-map-title">
    <div className="panel-heading"><div><h2 id="service-map-title"><Workflow aria-hidden="true" /> Mapa de sinais</h2><p>Um contexto para cada serviço.</p></div><span className="map-label">Topologia ilustrativa</span></div>
    <div className="map-canvas">
      <svg viewBox="0 0 700 260" preserveAspectRatio="none" className="map-wires" aria-hidden="true"><path d="M150 63 H260 Q280 63 280 83 V110 Q280 130 300 130 H350 M550 63 H440 Q420 63 420 83 V110 Q420 130 400 130 H350 M150 202 H260 Q280 202 280 182 V150 Q280 130 300 130 H350 M550 202 H440 Q420 202 420 182 V150 Q420 130 400 130 H350" /><circle cx="280" cy="130" r="3" /><circle cx="420" cy="130" r="3" /></svg>
      <div className="map-core"><div className="core-icon"><Fingerprint aria-hidden="true" /></div><strong>Investigator</strong><span>{incidents.length} sinais recebidos</span></div>
      {services.map(({ name, label, Icon }, index) => {
        const count = incidents.filter(item => item.source === name).length;
        return <button key={name} type="button" aria-label={`Filtrar serviço ${name}`} aria-pressed={service === name} onClick={() => onSelect(service === name ? '' : name)} className={cn('service-node', `service-node-${index}`, service === name && 'is-filtered')}>
          <span className="node-icon"><Icon aria-hidden="true" /></span><span className="node-copy"><strong>{label}</strong><code translate="no">{name}</code></span><span className="node-count">{count}</span>
        </button>;
      })}
    </div>
    <div className="map-caption"><span className="signal-dot" /> Contagens da janela selecionada<span>Selecione um serviço para filtrar</span></div>
  </section>;
}
