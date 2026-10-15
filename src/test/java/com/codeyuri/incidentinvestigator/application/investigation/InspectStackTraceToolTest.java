package com.codeyuri.incidentinvestigator.application.investigation;

import com.codeyuri.incidentinvestigator.domain.model.Incident;
import com.codeyuri.incidentinvestigator.domain.model.Severity;
import com.codeyuri.incidentinvestigator.domain.port.IncidentStore;
import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InspectStackTraceToolTest {
 @Test void returnsOnlyNormalizedFrames() {
  Incident incident=Incident.receive("Order endpoint fails", "java.lang.IllegalStateException\n  at a.Service.run(Service.java:10)\n at b.Api.call(Api.java:3)", "orders", Severity.HIGH);
  IncidentStore store=new IncidentStore(){ public Incident save(Incident i){return i;} public Optional<Incident> findById(UUID id){return Optional.of(incident);} };
  var result=new InspectStackTraceTool(store).execute(Map.of("incidentId", incident.getId().toString()), "c-1");
  assertThat(result.get("frames")).isEqualTo(java.util.List.of("a.Service.run(Service.java:10)", "b.Api.call(Api.java:3)"));
 }
 @Test void rejectsExtraParameters() {
  assertThatThrownBy(() -> new InspectStackTraceTool(null).execute(Map.of("incidentId", "x", "other", "x"), "c"))
   .isInstanceOf(IllegalArgumentException.class);
 }
}
