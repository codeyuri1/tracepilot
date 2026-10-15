package com.codeyuri.incidentinvestigator.infrastructure.ai;

import com.codeyuri.incidentinvestigator.application.analysis.AnalysisModelClient;
import com.codeyuri.incidentinvestigator.application.analysis.AnalysisModelOutput;
import com.codeyuri.incidentinvestigator.application.analysis.AnalysisModelTimeoutException;
import com.codeyuri.incidentinvestigator.application.analysis.AnalysisPrompt;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@Profile({"local & !demo & !showcase", "openai"})
public class SpringAiAnalysisClient implements AnalysisModelClient {
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final String model;
    private final Duration timeout;

    public SpringAiAnalysisClient(ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper,
                                  @Value("${app.analysis.model}") String model,
                                  @Value("${app.analysis.timeout:PT30S}") Duration timeout) {
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = objectMapper;
        this.model = model;
        this.timeout = timeout;
    }

    @Override
    public AnalysisModelOutput analyze(AnalysisPrompt prompt) {
        String content;
        try {
            content = CompletableFuture.supplyAsync(() -> chatClient.prompt().user(prompt.content()).call().content())
                    .orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                    .join();
        } catch (CompletionException exception) {
            if (exception.getCause() instanceof TimeoutException) {
                throw new AnalysisModelTimeoutException();
            }
            throw exception;
        }
        try {
            return objectMapper.readValue(content, AnalysisModelOutput.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Model response is not valid JSON", exception);
        }
    }

    @Override
    public String modelName() {
        return model;
    }
}
