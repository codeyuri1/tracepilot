package com.codeyuri.incidentinvestigator.application.usecase;

import com.codeyuri.incidentinvestigator.api.dto.AnalysisResponse;
import com.codeyuri.incidentinvestigator.application.analysis.AnalysisFailedException;
import com.codeyuri.incidentinvestigator.application.analysis.AnalysisModelClient;
import com.codeyuri.incidentinvestigator.application.analysis.AnalysisModelOutput;
import com.codeyuri.incidentinvestigator.application.analysis.AnalysisModelTimeoutException;
import com.codeyuri.incidentinvestigator.application.analysis.AnalysisOutputValidationException;
import com.codeyuri.incidentinvestigator.application.analysis.AnalysisOutputValidator;
import com.codeyuri.incidentinvestigator.application.analysis.AnalysisPrompt;
import com.codeyuri.incidentinvestigator.application.analysis.AnalysisPromptFactory;
import com.codeyuri.incidentinvestigator.application.analysis.AuditRedactor;
import com.codeyuri.incidentinvestigator.application.exception.IncidentNotFoundException;
import com.codeyuri.incidentinvestigator.application.security.TenantAccess;
import com.codeyuri.incidentinvestigator.domain.model.Analysis;
import com.codeyuri.incidentinvestigator.domain.model.Incident;
import com.codeyuri.incidentinvestigator.domain.port.AnalysisStore;
import com.codeyuri.incidentinvestigator.domain.port.IncidentStore;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.util.UUID;
import java.util.List;

@Service
public class AnalysisService {
    private final IncidentStore incidentStore;
    private final AnalysisStore analysisStore;
    private final ObjectProvider<AnalysisModelClient> analysisModelClient;
    private final AnalysisPromptFactory promptFactory;
    private final AnalysisOutputValidator outputValidator;
    private final AuditRedactor auditRedactor;
    private final TransactionTemplate transactionTemplate;
    private final MeterRegistry meterRegistry;

    public AnalysisService(IncidentStore incidentStore, AnalysisStore analysisStore,
                           ObjectProvider<AnalysisModelClient> analysisModelClient, AnalysisPromptFactory promptFactory,
                           AnalysisOutputValidator outputValidator, AuditRedactor auditRedactor,
                           TransactionTemplate transactionTemplate) {
        this(incidentStore, analysisStore, analysisModelClient, promptFactory, outputValidator, auditRedactor, transactionTemplate, null);
    }

    @Autowired
    public AnalysisService(IncidentStore incidentStore, AnalysisStore analysisStore,
                           ObjectProvider<AnalysisModelClient> analysisModelClient, AnalysisPromptFactory promptFactory,
                           AnalysisOutputValidator outputValidator, AuditRedactor auditRedactor,
                           TransactionTemplate transactionTemplate, MeterRegistry meterRegistry) {
        this.incidentStore = incidentStore;
        this.analysisStore = analysisStore;
        this.analysisModelClient = analysisModelClient;
        this.promptFactory = promptFactory;
        this.outputValidator = outputValidator;
        this.auditRedactor = auditRedactor;
        this.transactionTemplate = transactionTemplate;
        this.meterRegistry = meterRegistry;
    }

    public AnalysisResponse analyze(UUID incidentId) {
        Timer.Sample timer = meterRegistry == null ? null : Timer.start(meterRegistry);
        AnalysisExecution execution = transactionTemplate.execute(status -> start(incidentId));
        try {
            AnalysisModelOutput output = modelClient().analyze(execution.prompt());
            var evidence = outputValidator.validate(output, execution.suppliedMaterial());
            AnalysisResponse response = transactionTemplate.execute(status -> complete(execution.analysisId(), output, evidence));
            record("success", timer);
            return response;
        } catch (AnalysisModelTimeoutException exception) {
            fail(execution.analysisId(), "TIMEOUT", exception.getMessage());
            record("timeout", timer);
            throw new AnalysisFailedException();
        } catch (AnalysisOutputValidationException exception) {
            fail(execution.analysisId(), "VALIDATION_FAILED", exception.getMessage());
            record("validation_failed", timer);
            throw new AnalysisFailedException();
        } catch (RuntimeException exception) {
            fail(execution.analysisId(), "MODEL_UNAVAILABLE", exception.getMessage());
            record("model_unavailable", timer);
            throw new AnalysisFailedException();
        }
    }

    @Transactional(readOnly = true)
    public List<AnalysisResponse> history(UUID incidentId) {
        Incident incident = incidentStore.findById(incidentId).orElseThrow(() -> new IncidentNotFoundException(incidentId));
        if (!TenantAccess.currentTenant().equals(incident.getTenantId())) {
            throw new IncidentNotFoundException(incidentId);
        }
        return analysisStore.findByIncidentIdOrderByCreatedAtDesc(incidentId).stream().map(AnalysisResponse::from).toList();
    }

    private AnalysisExecution start(UUID incidentId) {
        Incident incident = incidentStore.findById(incidentId).orElseThrow(() -> new IncidentNotFoundException(incidentId));
        if (!TenantAccess.currentTenant().equals(incident.getTenantId())) {
            throw new IncidentNotFoundException(incidentId);
        }
        AnalysisPrompt prompt = promptFactory.create(incident);
        Analysis analysis = analysisStore.save(Analysis.start(
                incident, modelClient().modelName(), prompt.version(), auditRedactor.redact(prompt.content())));
        return new AnalysisExecution(analysis.getId(), prompt,
                incident.getTitle() + "\n" + incident.getSource() + "\n" + incident.getStackTrace());
    }

    private AnalysisResponse complete(UUID analysisId, AnalysisModelOutput output,
                                      java.util.List<Analysis.AnalysisEvidenceData> evidence) {
        Analysis analysis = analysisStore.findById(analysisId).orElseThrow();
        analysis.complete(output.category(), output.summary(), output.probableCause(), output.confidence(),
                output.recommendations() == null ? java.util.List.of() : output.recommendations(), evidence,
                auditRedactor.redact(output.toString()));
        return AnalysisResponse.from(analysisStore.save(analysis));
    }

    private void fail(UUID analysisId, String reason, String response) {
        transactionTemplate.executeWithoutResult(status -> {
            Analysis analysis = analysisStore.findById(analysisId).orElseThrow();
            analysis.fail(reason, auditRedactor.redact(response));
            analysisStore.save(analysis);
        });
    }

    private AnalysisModelClient modelClient() {
        return analysisModelClient.getIfAvailable(UnavailableAnalysisModelClient::new);
    }

    private void record(String outcome, Timer.Sample timer) {
        if (meterRegistry == null) return;
        Counter.builder("incident_investigator.analysis.requests").tag("outcome", outcome).register(meterRegistry).increment();
        timer.stop(Timer.builder("incident_investigator.analysis.latency").tag("outcome", outcome).register(meterRegistry));
    }

    private record AnalysisExecution(UUID analysisId, AnalysisPrompt prompt, String suppliedMaterial) { }

    private static class UnavailableAnalysisModelClient implements AnalysisModelClient {
        @Override
        public AnalysisModelOutput analyze(AnalysisPrompt prompt) {
            throw new IllegalStateException("No analysis model is configured");
        }

        @Override
        public String modelName() {
            return "unconfigured";
        }
    }
}
