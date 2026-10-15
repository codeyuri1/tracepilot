package com.codeyuri.incidentinvestigator.application.analysis;

import com.codeyuri.incidentinvestigator.domain.model.Incident;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AnalysisPromptFactory {
    private final String promptVersion;
    private final String outputSchema = new org.springframework.ai.converter.BeanOutputConverter<>(AnalysisModelOutput.class).getJsonSchema();

    public AnalysisPromptFactory(@Value("${app.analysis.prompt-version}") String promptVersion) {
        this.promptVersion = promptVersion;
    }

    public AnalysisPrompt create(Incident incident) {
        String prompt = """
                Você investiga um incidente técnico usando exclusivamente o material abaixo. Não alegue acesso a código-fonte,
                logs, sistemas de produção, incidentes relacionados ou documentação que não tenham sido fornecidos. Declare
                incerteza quando o material for insuficiente. Retorne apenas JSON que respeite o JSON_SCHEMA ao final, com category, summary, probableCause,
                confidence, evidence e recommendations. confidence deve ser um número entre 0 e 1, nunca texto como Alto ou Médio. Todos os campos textuais destinados a pessoas — summary,
                probableCause e recommendations — devem estar em português do Brasil. Cada item de evidence deve conter
                type, reference e um excerpt literal do material fornecido; nunca traduza, resuma ou invente esse excerpt.

                Separe observação de hipótese: summary descreve o sinal observado e o impacto que o material permite
                sustentar; probableCause apresenta hipóteses e os dados que faltam para confirmá-las. Não transforme
                severidade em prova de impacto global. Eventos recuperados por fallback não comprovam indisponibilidade.
                Em recommendations, retorne três ações específicas e ordenadas, com os prefixos Investigar:, Mitigar:
                e Validar:. Evite reinícios, limpeza de cache e novas tentativas indiscriminadas; considere idempotência.

                Severidade informada: %s
                Título do incidente: %s
                Origem: %s
                Stack trace:
                %s
                """.formatted(incident.getSeverity(), incident.getTitle(), incident.getSource(), incident.getStackTrace());
        return new AnalysisPrompt(promptVersion, prompt + "\nJSON_SCHEMA:\n" + outputSchema + "\nEND_JSON_SCHEMA");
    }
}
