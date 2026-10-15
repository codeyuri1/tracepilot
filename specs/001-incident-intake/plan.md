# Plano técnico

Camadas: `api` (contratos HTTP) → `application` (casos de uso) → `domain` (entidade e regras) → `infrastructure` (JPA). O controller não recebe nem retorna entidade JPA.

`IncidentService#create` é o limite `@Transactional`; leitura é `readOnly`. Flyway é a única fonte de mudanças no schema. `TEXT` preserva stack traces; índices vetoriais serão adicionados somente na spec 003.

Erro segue RFC 9457 com `ProblemDetail`, uma lista de `violations` e `traceId` quando disponível.
