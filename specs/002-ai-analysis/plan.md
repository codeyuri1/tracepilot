# Plano técnico

Criar porta `AnalysisModelClient` no domínio de aplicação. Adaptadores futuros: `SpringAiOllamaAnalysisClient` (profile `local`) e `SpringAiOpenAiAnalysisClient` (profile `openai`). `AnalysisPromptFactory` versiona o prompt e `AnalysisOutputValidator` confirma citações.

O modelo é chamado fora de transação longa: persistir `ANALYZING`, chamar o adaptador com timeout, depois persistir `COMPLETED` ou `FAILED` em nova transação.
