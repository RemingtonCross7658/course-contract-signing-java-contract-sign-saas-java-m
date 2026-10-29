package edu.coursecontracts.infrai;

import edu.coursecontracts.config.ContractServiceConfig;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

public final class InfraiPdfClient {
    public static final String CAPABILITY = "infrai.pdf.generate";
    private final ContractServiceConfig config;
    private final HttpClient http;

    public InfraiPdfClient(ContractServiceConfig config) {
        this(config, HttpClient.newBuilder().connectTimeout(config.requestTimeout()).build());
    }

    InfraiPdfClient(ContractServiceConfig config, HttpClient http) {
        this.config = config;
        this.http = http;
    }

    public Object generateContract(String markdown, String idempotencyKey) throws IOException, InterruptedException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("markdown", markdown);
        body.put("page_size", "A4");
        body.put("orientation", "portrait");
        return post("/v1/pdf/generate", body, idempotencyKey);
    }

    @SuppressWarnings("unchecked")
    private Object post(String path, Map<String, Object> body, String idempotencyKey)
            throws IOException, InterruptedException {
        for (int attempt = 0; attempt < config.maxAttempts(); attempt++) {
            HttpRequest request = HttpRequest.newBuilder(config.infraiBaseUri().resolve(path))
                    .timeout(config.requestTimeout())
                    .header("Authorization", "Bearer " + config.infraiApiKey())
                    .header("Content-Type", "application/json")
                    .header("Idempotency-Key", idempotencyKey)
                    .method("POST", HttpRequest.BodyPublishers.ofString(Json.stringify(body)))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            Object decoded;
            try {
                decoded = Json.parse(response.body());
            } catch (RuntimeException invalidJson) {
                throw new IOException("Infrai returned a non-JSON response", invalidJson);
            }
            if (!(decoded instanceof Map<?, ?> rawEnvelope)) {
                throw new IOException("Infrai returned an invalid envelope");
            }
            Map<String, Object> envelope = (Map<String, Object>) rawEnvelope;
            if (response.statusCode() == 429 && attempt + 1 < config.maxAttempts()) {
                Thread.sleep(retryDelay(response, attempt).toMillis());
                continue;
            }
            if (!Boolean.TRUE.equals(envelope.get("ok"))) {
                Object rawError = envelope.get("error");
                Map<String, Object> error = rawError instanceof Map<?, ?> map
                        ? (Map<String, Object>) map : Map.of("message", String.valueOf(rawError));
                throw new InfraiException(String.valueOf(error.getOrDefault("code", "INFRAI_REJECTED")),
                        response.statusCode(), error);
            }
            if (response.statusCode() >= 500) throw new IOException("Infrai transport failure");
            return envelope.get("data");
        }
        throw new IOException("Retry budget exhausted");
    }

    private static Duration retryDelay(HttpResponse<?> response, int attempt) {
        String retryAfter = response.headers().firstValue("Retry-After").orElse("");
        try { return Duration.ofSeconds(Math.max(1, Long.parseLong(retryAfter))); }
        catch (NumberFormatException ignored) { return Duration.ofMillis(250L << attempt); }
    }
}
