# IncidentAnalysisRequested v1

Kafka is deliberately not a dependency yet. When analysis execution becomes asynchronous, the transaction that creates
an analysis must write this event to an outbox in the same PostgreSQL transaction. A relay may later publish it to Kafka.

| Field | Rule |
|---|---|
| `eventId` | UUID generated once and retained by the outbox relay |
| `eventType` | Literal `IncidentAnalysisRequested.v1` |
| `occurredAt` | UTC instant of the committed request |
| `tenantId` | Required; consumers must scope every lookup to it |
| `incidentId` / `analysisId` | UUIDs; together form the idempotency key |
| `correlationId` | Required opaque trace identifier, never an email or access token |

The payload contains only identifiers and version metadata—never a stack trace, prompt, model response, API key, or
JWT. Consumers persist processed `eventId`s before side effects. Failed deliveries use exponential backoff and, after a
bounded retry count, move the original envelope plus error metadata to a DLQ; replaying a DLQ item retains its
`eventId` and is therefore idempotent.
