import { useCallback, useEffect, useRef, useState } from 'react';
import { createIncidentApi } from '@/lib/api';
import type { Analysis, Incident, Scenario } from '@/lib/types';

export function useInvestigation() {
  const [api] = useState(createIncidentApi);
  const [incidents, setIncidents] = useState<Incident[]>([]);
  const [selectedId, setSelectedId] = useState(() => new URLSearchParams(location.search).get('incident') ?? '');
  const [analysis, setAnalysis] = useState<Analysis>();
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [busy, setBusy] = useState(false);
  const [paused, setPaused] = useState(false);
  const [error, setError] = useState('');
  const [connectionError, setConnectionError] = useState('');
  const [message, setMessage] = useState('');
  const [updatedAt, setUpdatedAt] = useState<number>();
  const [revision, setRevision] = useState(0);
  const mounted = useRef(false);
  const pending = useRef(false);
  const mutating = useRef(false);
  const listController = useRef<AbortController | null>(null);
  const refresh = useCallback(async () => {
    if (pending.current || mutating.current) return;
    pending.current = true;
    const controller = new AbortController();
    listController.current = controller;
    setRefreshing(true);
    try {
      const items = await api.list(controller.signal);
      if (!mounted.current || controller.signal.aborted || mutating.current) return;
      setIncidents(items);
      setSelectedId(current => items.some(item => item.id === current) ? current : items[0]?.id ?? '');
      setUpdatedAt(Date.now());
      setRevision(value => value + 1);
      setConnectionError('');
    } catch (failure) {
      if (mounted.current && !controller.signal.aborted) setConnectionError(failure instanceof Error ? failure.message : 'Não foi possível atualizar. Tente novamente.');
    } finally {
      if (listController.current === controller) {
        pending.current = false;
        if (mounted.current && !controller.signal.aborted) { setLoading(false); setRefreshing(false); }
      }
    }
  }, [api]);
  useEffect(() => {
    mounted.current = true;
    void refresh();
    return () => { mounted.current = false; listController.current?.abort(); pending.current = false; };
  }, [refresh]);
  useEffect(() => {
    if (paused) return;
    const timer = window.setInterval(() => void refresh(), 5000);
    return () => window.clearInterval(timer);
  }, [paused, refresh]);
  useEffect(() => {
    if (!selectedId || busy) return;
    const controller = new AbortController();
    void api.history(selectedId, controller.signal).then(history => {
      if (!controller.signal.aborted) setAnalysis(history.find(item => item.category && item.summary));
    }).catch(failure => {
      if (!controller.signal.aborted) setError(failure instanceof Error ? failure.message : 'Não foi possível carregar a análise. Tente novamente.');
    });
    return () => controller.abort();
  }, [api, selectedId, revision, busy]);
  function select(id: string) {
    if (mutating.current || id === selectedId) return;
    setAnalysis(undefined);
    setSelectedId(id);
    setMessage('');
    const url = new URL(location.href); url.searchParams.set('incident', id); history.replaceState(null, '', url);
  }
  async function ingest(scenario: Scenario) {
    if (mutating.current) return false;
    listController.current?.abort(); pending.current = false; setRefreshing(false);
    mutating.current = true; setBusy(true); setError('');
    try {
      const created = await api.ingest(scenario);
      if (!mounted.current) return false;
      setIncidents(items => [created, ...items.filter(item => item.id !== created.id)]);
      setSelectedId(created.id); setAnalysis(undefined); setUpdatedAt(Date.now()); setLoading(false); setConnectionError('');
      setMessage('Incidente criado. Selecione Analisar incidente para investigar.');
      return true;
    } catch (failure) {
      if (mounted.current) setError(failure instanceof Error ? failure.message : 'Não foi possível criar o incidente. Tente novamente.');
      return false;
    } finally { mutating.current = false; if (mounted.current) setBusy(false); }
  }
  async function analyze() {
    if (!selectedId || mutating.current) return;
    listController.current?.abort(); pending.current = false; setRefreshing(false);
    mutating.current = true; setBusy(true); setError(''); setMessage('Analisando o material fornecido…');
    try {
      const result = await api.analyze(selectedId);
      if (mounted.current) { setAnalysis(result); setMessage('Análise concluída com evidências verificadas.'); }
    } catch (failure) {
      if (mounted.current) { setError(failure instanceof Error ? failure.message : 'Não foi possível analisar. Tente novamente.'); setMessage(''); }
    } finally { mutating.current = false; if (mounted.current) setBusy(false); }
  }
  return { incidents, selectedId, incident: incidents.find(item => item.id === selectedId), analysis, loading, refreshing, busy, paused, setPaused, error: error || connectionError, message, updatedAt, refresh, retry: () => { setError(''); void refresh(); }, select, ingest, analyze };
}
