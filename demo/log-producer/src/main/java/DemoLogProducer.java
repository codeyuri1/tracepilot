import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/** Simulates an external production producer through the public API, never through the database. */
public final class DemoLogProducer {
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private static final List<String> EVENTS = List.of(
            "{\"service\":\"payment-worker\",\"level\":\"ERROR\",\"message\":\"Falha ao conectar no PostgreSQL\",\"stackTrace\":\"org.postgresql.util.PSQLException: Connection refused\"}",
            "{\"service\":\"checkout-api\",\"level\":\"WARN\",\"message\":\"Tempo limite na autorização de pagamento\",\"stackTrace\":\"java.net.SocketTimeoutException: Read timed out\"}",
            "{\"service\":\"auth-api\",\"level\":\"ERROR\",\"message\":\"Dependência de identidade indisponível\",\"stackTrace\":\"HTTP 503 Service Unavailable from identity-provider\"}",
            "{\"service\":\"orders-worker\",\"level\":\"ERROR\",\"message\":\"Worker não conseguiu processar o pedido\",\"stackTrace\":\"java.lang.IllegalStateException: order state is invalid\"}",
            "{\"service\":\"orders-worker\",\"level\":\"INFO\",\"message\":\"Cache ausente; consulta recuperada pelo fallback\",\"stackTrace\":\"demo.cache.CacheMiss: cache miss; fallback completed\"}");

    public static void main(String[] args) throws Exception {
        String base = System.getenv().getOrDefault("APP_URL", "http://app:8080");
        String shared = System.getenv().getOrDefault("SHARED_DIR", "");
        boolean continuous = Boolean.parseBoolean(System.getenv().getOrDefault("SIMULATION_CONTINUOUS", "true"));
        boolean analyze = Boolean.parseBoolean(System.getenv().getOrDefault("SIMULATION_ANALYZE", "true"));
        long intervalMillis = Long.parseLong(System.getenv().getOrDefault("SIMULATION_INTERVAL_MS", "10000"));
        waitForHealth(base);
        int index = 0;
        do {
            try {
                String writer = token(base, "INCIDENT_WRITER");
                HttpResponse<String> created = post(base + "/api/v1/demo/log-events", writer, EVENTS.get(index++ % EVENTS.size()));
                if (created.statusCode() != 201) throw new IllegalStateException("ingestion returned " + created.statusCode());
                if (!shared.isBlank()) {
                    Files.createDirectories(Path.of(shared));
                    Files.writeString(Path.of(shared, "incident.json"), created.body());
                }
                if (analyze) {
                    String incidentId = body(created, "id");
                    HttpResponse<String> result = post(base + "/api/v1/incidents/" + incidentId + "/analyses", token(base, "ANALYST"), "");
                    if (result.statusCode() != 201) throw new IllegalStateException("analysis returned " + result.statusCode());
                }
            } catch (Exception exception) {
                System.err.println("simulation cycle failed: " + exception.getMessage());
            }
            if (continuous) Thread.sleep(intervalMillis);
        } while (continuous);
    }

    static void waitForHealth(String base) throws Exception { for (int i = 0; i < 60; i++) { try { if (get(base + "/actuator/health").statusCode() == 200) return; } catch (Exception ignored) { } Thread.sleep(1000); } throw new IllegalStateException("health check timed out"); }
    static HttpResponse<String> get(String url) throws Exception { return HTTP.send(HttpRequest.newBuilder(URI.create(url)).GET().build(), HttpResponse.BodyHandlers.ofString()); }
    static String token(String base, String role) throws Exception { return body(get(base + "/local/demo-token?role=" + role + "&tenant=demo"), "accessToken"); }
    static HttpResponse<String> post(String url, String token, String payload) throws Exception { return HTTP.send(HttpRequest.newBuilder(URI.create(url)).header("Authorization", "Bearer " + token).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(payload)).build(), HttpResponse.BodyHandlers.ofString()); }
    static String body(HttpResponse<String> response, String field) { if (response.statusCode() != 200 && response.statusCode() != 201) throw new IllegalStateException("request returned " + response.statusCode()); return response.body().replaceFirst("(?s).*\\\"" + field + "\\\"\\s*:\\s*\\\"([^\\\"]+)\\\".*", "$1"); }
}
