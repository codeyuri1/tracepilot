package com.codeyuri.incidentinvestigator.api.error;

import com.codeyuri.incidentinvestigator.application.exception.IncidentNotFoundException;
import com.codeyuri.incidentinvestigator.application.analysis.AnalysisFailedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleInvalidArgument(IllegalArgumentException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid request");
        problem.setType(URI.create("https://api.incident-investigator.dev/problems/invalid-request"));
        problem.setTitle("Invalid request");
        return problem;
    }

    @ExceptionHandler(AnalysisFailedException.class)
    ProblemDetail handleAnalysisFailure(AnalysisFailedException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "Analysis could not be completed");
        problem.setType(URI.create("https://api.incident-investigator.dev/problems/analysis-unavailable"));
        problem.setTitle("Analysis unavailable");
        return problem;
    }

    @ExceptionHandler(IncidentNotFoundException.class)
    ProblemDetail handleNotFound(IncidentNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setType(URI.create("https://api.incident-investigator.dev/problems/incident-not-found"));
        problem.setTitle("Incident not found");
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
        problem.setType(URI.create("https://api.incident-investigator.dev/problems/validation"));
        problem.setTitle("Invalid request");
        List<Map<String, String>> violations = exception.getBindingResult().getFieldErrors().stream()
                .map(this::violation)
                .toList();
        problem.setProperty("violations", violations);
        return problem;
    }

    private Map<String, String> violation(FieldError error) {
        return Map.of("field", error.getField(), "message", error.getDefaultMessage());
    }
}
