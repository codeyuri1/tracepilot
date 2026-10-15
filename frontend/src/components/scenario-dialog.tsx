import { useState } from 'react';
import { Database, Timer, ShieldAlert, Layers3, Plus, LoaderCircle, CheckCircle2 } from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle, DialogTrigger, DialogFooter } from '@/components/ui/dialog';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { FieldSet, FieldLegend, FieldLabel } from '@/components/ui/field';
import { scenarios, severityNames, scenarioSeverity, type Scenario } from '@/lib/types';
const icons = [Database, Timer, ShieldAlert, Layers3, CheckCircle2];
export function ScenarioDialog({ busy, error, onIngest }: { busy: boolean; error: string; onIngest: (scenario: Scenario) => Promise<boolean> }) {
  const [open, setOpen] = useState(false);
  const [choice, setChoice] = useState('database');
  const [failed, setFailed] = useState(false);
  return <Dialog open={open} onOpenChange={value => { if (!busy) { setOpen(value); setFailed(false); } }}><DialogTrigger asChild><Button disabled={busy}><Plus aria-hidden="true" data-icon="inline-start" />Novo incidente</Button></DialogTrigger>
    <DialogContent className="scenario-dialog"><DialogHeader><DialogTitle>Simular um incidente</DialogTitle><DialogDescription>Escolha um cenário. O evento será enviado à API com dados inteiramente sintéticos.</DialogDescription></DialogHeader>
      <FieldSet><FieldLegend className="sr-only">Escolha o cenário</FieldLegend><div className="scenario-options">{scenarios.map((scenario, index) => { const Icon = icons[index]; return <FieldLabel key={scenario.id} className="scenario-option"><input type="radio" name="scenario" value={scenario.id} checked={choice === scenario.id} onChange={() => setChoice(scenario.id)} disabled={busy} /><Icon aria-hidden="true" /><span><strong>{scenario.name}<Badge variant="outline" className={`severity-badge severity-${scenarioSeverity(scenario.level).toLowerCase()}`}>{severityNames[scenarioSeverity(scenario.level)]}</Badge></strong><small>{scenario.description}</small></span></FieldLabel>; })}</div></FieldSet>
      <div className="scenario-preview"><span>Prévia do evento</span><code translate="no">{scenarios.find(item => item.id === choice)?.stackTrace}</code></div>
      {failed ? <Alert variant="destructive"><AlertDescription>{error || 'Não foi possível criar o incidente. Confira a conexão e tente novamente.'}</AlertDescription></Alert> : null}
      <DialogFooter><Button variant="outline" disabled={busy} onClick={() => setOpen(false)}>Cancelar</Button><Button disabled={busy} onClick={async () => { setFailed(false); const scenario = scenarios.find(item => item.id === choice)!; if (await onIngest(scenario)) setOpen(false); else setFailed(true); }}>{busy ? <LoaderCircle className="spin" aria-hidden="true" data-icon="inline-start" /> : <Plus aria-hidden="true" data-icon="inline-start" />}{busy ? 'Enviando…' : 'Injetar incidente'}</Button></DialogFooter>
    </DialogContent>
  </Dialog>;
}
