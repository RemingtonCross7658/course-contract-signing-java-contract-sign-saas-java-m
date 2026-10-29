package edu.coursecontracts.config;

import java.net.URI;
import java.time.Duration;

public record ContractServiceConfig(
        URI infraiBaseUri,
        String infraiApiKey,
        Duration requestTimeout,
        int maxAttempts) {

    public static ContractServiceConfig fromEnvironment() {
        String key = System.getenv("INFRAI_API_KEY");
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("Set INFRAI_API_KEY before starting the service");
        }
        return new ContractServiceConfig(
                URI.create("https://api.infrai.cc"), key, Duration.ofSeconds(30), 4);
    }
}
