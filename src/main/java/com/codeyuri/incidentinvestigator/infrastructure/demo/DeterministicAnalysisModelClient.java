package com.codeyuri.incidentinvestigator.infrastructure.demo;

import com.codeyuri.incidentinvestigator.application.analysis.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.List;

/** Local-only scenarios: observations are literal; explanations remain hypotheses, not verified root causes. */
@Component
@Profile({"demo", "showcase"})
public class DeterministicAnalysisModelClient implements AnalysisModelClient {
    @Override
    public AnalysisModelOutput analyze(AnalysisPrompt prompt) {
        if (prompt.content().contains("Connection refused")) {
            return result("DATABASE_CONNECTIVITY",
                    "A tentativa de conexão com o PostgreSQL foi recusada. A operação que depende dessa conexão pode ter sido interrompida; o alcance do impacto ainda precisa ser confirmado.",
                    "O destino pode estar sem um listener ativo, com endereço ou porta incorretos, ou com rejeição de rede. O trecho não distingue essas hipóteses e não comprova falha de senha nem indisponibilidade de todo o banco.",
                    "Connection refused", "0.90",
                    "Investigar: confira host e porta configurados, teste a conexão a partir do mesmo serviço e consulte a saúde e os logs do PostgreSQL no horário do evento.",
                    "Mitigar: se a dependência estiver indisponível, contenha as novas tentativas com backoff e circuit breaker; preserve os eventos pendentes antes de reprocessar.",
                    "Validar: execute uma conexão e uma operação controlada, confira o retorno do processamento e observe se novas recusas continuam aparecendo.");
        }
        if (prompt.content().contains("Read timed out")) {
            return result("DEPENDENCY_TIMEOUT",
                    "A leitura da resposta excedeu o tempo limite configurado. Há um sinal de degradação, mas o log não confirma se a operação remota falhou ou foi concluída.",
                    "Lentidão na dependência, saturação de conexões ou atraso de rede são hipóteses. Sem duração, métricas e resposta remota, não é possível identificar qual delas ocorreu.",
                    "Read timed out", "0.85",
                    "Investigar: correlacione o horário e o identificador da requisição com a latência da dependência, o pool de conexões e os timeouts configurados.",
                    "Mitigar: limite a concorrência e use backoff dentro do orçamento de tentativas; em pagamentos, confirme o resultado e a chave de idempotência antes de repetir a operação.",
                    "Validar: faça uma requisição controlada, verifique o resultado na dependência e acompanhe se a latência e os timeouts voltaram ao comportamento esperado.");
        }
        if (prompt.content().contains("HTTP 503 Service Unavailable")) {
            return result("DEPENDENCY_UNAVAILABLE",
                    "A dependência respondeu HTTP 503 nesta chamada. A requisição não recebeu uma resposta normal; o evento isolado não demonstra uma indisponibilidade global.",
                    "Sobrecarga, manutenção ou falha de uma dependência interna podem explicar o 503. O status por si só não identifica a causa nem sua duração.",
                    "HTTP 503 Service Unavailable", "0.85",
                    "Investigar: correlacione a resposta 503 com a saúde do provedor de identidade, seus logs e o volume de chamadas no mesmo período.",
                    "Mitigar: respeite Retry-After quando presente, aplique circuit breaker e backoff; não desabilite a autenticação para contornar a falha.",
                    "Validar: teste o fluxo de autenticação com uma conta sintética e confirme que novas requisições recebem respostas válidas sem repetição de 503.");
        }
        if (prompt.content().contains("order state is invalid")) {
            return result("INVALID_STATE",
                    "O worker rejeitou o estado de um pedido durante o processamento. É necessário identificar qual pedido e qual transição foram afetados antes de reprocessar.",
                    "Uma transição fora de ordem, um evento duplicado ou um estado desatualizado podem explicar a exceção. O trecho não informa o estado anterior nem comprova corrupção de dados.",
                    "order state is invalid", "0.85",
                    "Investigar: localize o pedido e o evento, compare o estado persistido com a transição solicitada e verifique a ordem e a duplicidade dos eventos.",
                    "Mitigar: isole o evento para revisão e evite tentativas ilimitadas; só reprocesse após confirmar a transição permitida e a proteção por idempotência.",
                    "Validar: reproduza a transição com dados sintéticos e confirme que o pedido é processado uma única vez, preservando os estados válidos.");
        }
        if (prompt.content().contains("cache miss; fallback completed")) {
            return result("RECOVERED_CACHE_MISS",
                    "O item não estava no cache, mas o fallback foi concluído. O registro indica recuperação desta operação, não uma falha definitiva do atendimento.",
                    "Expiração, cache frio ou ausência prévia da chave são hipóteses. Não há evidência de indisponibilidade no material fornecido; a frequência e o custo do fallback ainda precisam ser observados.",
                    "cache miss; fallback completed", "0.85",
                    "Investigar: confira a frequência de cache misses, o TTL e o tempo do fallback antes de decidir se há degradação recorrente.",
                    "Mitigar: se os misses forem recorrentes e custosos, avalie aquecer apenas as chaves necessárias; não limpe todo o cache nem reinicie serviços por este evento isolado.",
                    "Validar: repita a consulta de forma controlada e confirme o resultado correto, a reutilização do cache quando aplicável e a ausência de novos erros.");
        }
        // Unknown material exercises explicit rejection of unsupported evidence, never a fabricated successful diagnosis.
        return new AnalysisModelOutput("UNKNOWN", "Material insuficiente para um diagnóstico confiável.", "Não há causa verificada.", new BigDecimal("0.10"),
                List.of(new AnalysisEvidenceCandidate("STACK_TRACE", "incident.stackTrace", "not present in supplied material")),
                List.of("Colete logs adicionais antes de tomar uma ação corretiva."));
    }
    private AnalysisModelOutput result(String category, String summary, String cause, String excerpt, String confidence, String... actions) {
        return new AnalysisModelOutput(category, summary, cause, new BigDecimal(confidence),
                List.of(new AnalysisEvidenceCandidate("STACK_TRACE", "incident.stackTrace", excerpt)), List.of(actions));
    }
    @Override public String modelName() { return "deterministic-demo-model"; }
}
