package com.codeyuri.incidentinvestigator.evaluation.retrieval;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RetrievalQualityGateTest {
    @Test
    void acceptsBaselineAndRejectsRegression() {
        RetrievalQualityGate gate = new RetrievalQualityGate(.80, .70, .60);
        assertThatCode(() -> gate.verify(.80, .70, .60)).doesNotThrowAnyException();
        assertThatThrownBy(() -> gate.verify(.79, .70, .60)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("quality gate failed");
    }
}
