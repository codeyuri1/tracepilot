package com.codeyuri.incidentinvestigator.application.investigation;

import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.Set;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InvestigationOrchestratorTest {
 @Test void cachesRepeatedCallsAndAuditsThem() {
  AtomicInteger calls=new AtomicInteger(); InvestigationTool tool=new Tool(){ public Map<String,Object> execute(Map<String,Object> p,String c){calls.incrementAndGet();return Map.of("ok",true);} };
  var orchestrator=new InvestigationOrchestrator(new ToolRegistry(Set.of(tool)),new InvestigationBudget(1,100));
  orchestrator.execute(new ToolCall("safe",Map.of("q","x")),"c"); orchestrator.execute(new ToolCall("safe",Map.of("q","x")),"c");
  assertThat(calls).hasValue(1); assertThat(orchestrator.audit()).extracting(ToolInvocation::cached).containsExactly(false,true);
 }
 @Test void rejectsUnknownToolsAndExhaustedBudget() {
  var orchestrator=new InvestigationOrchestrator(new ToolRegistry(Set.of(new Tool())),new InvestigationBudget(1,100));
  assertThatThrownBy(() -> orchestrator.execute(new ToolCall("shell",Map.of()),"c")).isInstanceOf(IllegalArgumentException.class);
  orchestrator.execute(new ToolCall("safe",Map.of("a",1)),"c");
  assertThatThrownBy(() -> orchestrator.execute(new ToolCall("safe",Map.of("a",2)),"c")).isInstanceOf(IllegalStateException.class);
  assertThat(orchestrator.audit()).extracting(ToolInvocation::outcome).contains("CALL_BUDGET_EXHAUSTED");
 }
 @Test void recordsAndStopsTimedOutTools() {
  InvestigationTool slow = new Tool() { public Map<String,Object> execute(Map<String,Object> p, String c) {
   try { Thread.sleep(100); } catch (InterruptedException exception) { Thread.currentThread().interrupt(); }
   return Map.of();
  }};
  var orchestrator = new InvestigationOrchestrator(new ToolRegistry(Set.of(slow)), new InvestigationBudget(1, 100), Duration.ofMillis(10));
  assertThatThrownBy(() -> orchestrator.execute(new ToolCall("safe", Map.of()), "c")).isInstanceOf(IllegalStateException.class)
   .hasMessageContaining("timed out");
  assertThat(orchestrator.audit()).extracting(ToolInvocation::outcome).containsExactly("TIMEOUT");
 }
 @Test void propagatesAuthenticatedTenantToToolWorker() {
  var jwt = org.springframework.security.oauth2.jwt.Jwt.withTokenValue("synthetic-token")
    .header("alg", "HS256").claim("tenant", "team-a").build();
  var authentication = new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken(jwt, java.util.List.of());
  org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(authentication);
  try {
   InvestigationTool tool = new Tool() { public Map<String,Object> execute(Map<String,Object> p, String c) {
    return Map.of("tenant", com.codeyuri.incidentinvestigator.application.security.TenantAccess.currentTenant());
   }};
   var orchestrator = new InvestigationOrchestrator(new ToolRegistry(Set.of(tool)), new InvestigationBudget(1, 100));
   assertThat(orchestrator.execute(new ToolCall("safe", Map.of()), "correlation")).containsEntry("tenant", "team-a");
  } finally { org.springframework.security.core.context.SecurityContextHolder.clearContext(); }
 }
 static class Tool implements InvestigationTool { public String name(){return "safe";} public Map<String,Object> execute(Map<String,Object> p,String c){return Map.of();} }
}
