package com.codeyuri.incidentinvestigator.infrastructure.demo;

import com.codeyuri.incidentinvestigator.application.intake.LogEventIngestionService;
import com.codeyuri.incidentinvestigator.domain.port.IncidentStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** Enables the synthetic log intake boundary for deterministic and real-LLM demonstrations. */
@Configuration
@Profile({"showcase", "llm"})
public class DemoLogIngestionConfiguration {
    @Bean
    LogEventIngestionService logEventIngestionService(IncidentStore store) {
        return new LogEventIngestionService(store);
    }
}
