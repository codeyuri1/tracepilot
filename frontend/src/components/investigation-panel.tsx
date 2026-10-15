import { cn } from 'cn';
import { useState } from 'react';
import { Sparkles, ArrowUpRight, ShieldCheck, Check, Copy, Download, ScanLine, LoaderCircle, FileCode2, ListChecks, Fingerprint } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs';
import { Progress } from '@/components/ui/progress';
import { Empty, EmptyHeader, EmptyTitle, EmptyDescription, EmptyMedia } from '@/components/ui/empty';
import { Tooltip, TooltipContent, TooltipTrigger } from '@/components/ui/tooltip';
import { categoryNames, severityNames, time, type Incident, type Analysis } from '@/lib/types';

export function InvestigationPanel({ incident, analysis, busy, analyze }: { incident?: Incident; analysis?: Analysis; busy: boolean; analyze: () => void }) {
  const isDemo = Boolean(analysis?.model.startsWith('deterministic-'));
  const [checked, setChecked] = useState<string[]>([]);
  const [copyStatus, setCopyStatus] = useState('');
  async function copy() {
    try { await navigator.clipboard.writeText(JSON.stringify(analysis, null, 2)); setCopyStatus('Análise copiada.'); }
    catch { setCopyStatus('Não foi possível copiar. Use Exportar análise para salvar o resultado.'); }
  }
  function download() {
    const url = URL.createObjectURL(new Blob([JSON.stringify(analysis, null, 2)], { type: 'application/json' }));
    const link = document.createElement('a'); link.href = url; link.download = `analise-${incident?.id}.json`; link.click(); URL.revokeObjectURL(url);
  }
  return <section className="investigation-panel panel" aria-labelledby="investigation-title" aria-busy={busy}>
    <div className="panel-heading"><div><h2 id="investigation-title"><Sparkles aria-hidden="true" /> Investigação</h2><p>Da evidência à próxima ação.</p></div><Badge variant="secondary"><Fingerprint aria-hidden="true" data-icon="inline-start" />{analysis ? (isDemo ? 'Modo demonstração' : 'LLM real') : 'Análise estruturada'}</Badge></div>
    {!incident ? <Empty className="min-h-80"><EmptyHeader><EmptyMedia variant="icon"><ScanLine aria-hidden="true" /></EmptyMedia><EmptyTitle>O próximo sinal começa aqui</EmptyTitle><EmptyDescription>Crie um incidente ou aguarde o simulador para iniciar uma investigação.</EmptyDescription></EmptyHeader></Empty> : <>
      <div className="selected-incident"><div><span className="incident-reference" translate="no">INC-{incident.id.slice(0, 8).toUpperCase()}</span><Badge variant="outline" className={cn("severity-badge", `severity-${incident.severity.toLowerCase()}`)}>Nível {severityNames[incident.severity] ?? incident.severity}</Badge></div><h3>{incident.title.replace(`${incident.source}: `, '')}</h3><div className="selected-meta"><code translate="no">{incident.source}</code><span>Recebido às {time(incident.createdAt)}</span></div></div>
      <Tabs defaultValue="diagnosis" className="investigation-tabs"><TabsList variant="line" aria-label="Detalhes da investigação"><TabsTrigger value="diagnosis"><Sparkles aria-hidden="true" data-icon="inline-start" />Diagnóstico</TabsTrigger><TabsTrigger value="evidence"><FileCode2 aria-hidden="true" data-icon="inline-start" />Evidências {analysis ? <span className="tab-count">{analysis.evidence.length}</span> : null}</TabsTrigger><TabsTrigger value="actions"><ListChecks aria-hidden="true" data-icon="inline-start" />Ações</TabsTrigger></TabsList>
        <TabsContent value="diagnosis"><div className="diagnosis-content">
          {busy ? <div className="analysis-progress" role="status"><div className="analysis-orbit"><LoaderCircle className="spin" aria-hidden="true" /></div><h3>Analisando o material…</h3><p>Validando o diagnóstico e os trechos de evidência fornecidos.</p></div> : analysis ? <>
            <div className="diagnosis-label"><span className="verified-label"><ShieldCheck aria-hidden="true" />Evidência verificada</span><code translate="no">{analysis.category}</code></div>
            <h3 className="diagnosis-title">{categoryNames[analysis.category] ?? analysis.category}</h3><p className="analysis-summary">{analysis.summary}</p>
            <div className="priority-context" data-severity={incident.severity}><strong>{severityNames[incident.severity] ?? incident.severity} · Prioridade de investigação</strong><p>{incident.severity === 'LOW' ? 'Acompanhe recorrência e custo da recuperação. Este nível não indica, por si só, indisponibilidade.' : incident.severity === 'MEDIUM' ? 'Investigue a degradação e confirme o resultado da operação antes de repetir tentativas.' : 'Priorize a triagem e confirme o alcance do impacto antes de qualquer intervenção.'}</p></div>
            <div className="cause-block"><span>Hipótese de causa</span><p>{analysis.probableCause}</p></div>
            <div className="confidence-block"><div><span>{isDemo ? 'Confiança do cenário' : 'Confiança informada pelo modelo'}</span><Tooltip><TooltipTrigger asChild><button type="button" className="confidence-value" aria-label="Sobre a confiança da análise">{Math.round(analysis.confidence * 100)}%<ArrowUpRight aria-hidden="true" /></button></TooltipTrigger><TooltipContent>Estimativa informada na análise; não é uma probabilidade calibrada.</TooltipContent></Tooltip></div><Progress value={analysis.confidence * 100} aria-label="Confiança do cenário" /><p>{isDemo ? "Demo determinística. Cada conclusão cita o material recebido." : "Estimativa do modelo, sem calibração estatística. Confira as evidências antes de agir."}</p><div className="analysis-model"><span>Modelo da análise</span><code translate="no">{analysis.model}</code></div></div>
            {analysis.recommendations.length ? <div className="next-step"><span>Comece por aqui</span><p>{analysis.recommendations[0].replace(/^Investigar:\s*/, '')}</p></div> : null}
            {analysis.evidence[0] ? <blockquote className="evidence-preview"><FileCode2 aria-hidden="true" /><div><code translate="no">{analysis.evidence[0].excerpt}</code><cite>{analysis.evidence[0].reference}</cite></div><Check aria-hidden="true" /></blockquote> : null}
          </> : <Empty className="min-h-64"><EmptyHeader><EmptyMedia variant="icon"><ScanLine aria-hidden="true" /></EmptyMedia><EmptyTitle>Pronto para investigar</EmptyTitle><EmptyDescription>Solicite uma análise para encontrar a causa provável e verificar as evidências deste incidente.</EmptyDescription></EmptyHeader></Empty>}
        </div></TabsContent>
        <TabsContent value="evidence"><div className="evidence-content"><h3>Trechos que sustentam o diagnóstico</h3><p>A evidência deve existir no material fornecido. O stack trace completo não é exposto.</p>{analysis?.evidence.length ? analysis.evidence.map((item, index) => <div className="evidence-detail" key={`${item.reference}-${index}`}><span><FileCode2 aria-hidden="true" /><code translate="no">{item.reference}</code><Badge variant="secondary">Verificada</Badge></span><pre translate="no"><code>{item.excerpt}</code></pre></div>) : <p>As evidências aparecem após uma análise válida.</p>}</div></TabsContent>
        <TabsContent value="actions"><div className="actions-content"><h3>Próximas ações</h3><p>Use esta lista para acompanhar sua investigação nesta sessão. Ela não executa alterações nos serviços.</p>{analysis?.recommendations.length ? <ul className="action-checklist">{analysis.recommendations.map((item, index) => <li key={item}><label><input type="checkbox" checked={checked.includes(item)} onChange={event => setChecked(current => event.target.checked ? [...current, item] : current.filter(value => value !== item))} /><span><small>{/^(Investigar|Mitigar|Validar):/.test(item) ? item.split(":")[0] : `Ação ${index + 1}`}</small>{item.replace(/^(Investigar|Mitigar|Validar):\s*/, "")}</span></label></li>)}</ul> : <p>Analise o incidente para obter recomendações.</p>}</div></TabsContent>
      </Tabs>
      <div className="investigation-actions"><Button disabled={busy} onClick={analyze}>{busy ? <LoaderCircle aria-hidden="true" className="spin" data-icon="inline-start" /> : <Sparkles aria-hidden="true" data-icon="inline-start" />}{busy ? 'Analisando…' : 'Analisar incidente'}</Button><div>{analysis ? <><Tooltip><TooltipTrigger asChild><Button variant="ghost" size="icon" aria-label="Copiar análise" onClick={() => void copy()}><Copy aria-hidden="true" /></Button></TooltipTrigger><TooltipContent>Copiar análise</TooltipContent></Tooltip><Tooltip><TooltipTrigger asChild><Button variant="ghost" size="icon" aria-label="Exportar análise" onClick={download}><Download aria-hidden="true" /></Button></TooltipTrigger><TooltipContent>Exportar JSON</TooltipContent></Tooltip></> : <span className="safe-data-label"><ShieldCheck aria-hidden="true" />Dados sanitizados</span>}</div></div>
      <p className="sr-only" role="status">{copyStatus}</p>
    </>}
  </section>;
}
