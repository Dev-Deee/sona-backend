package io.trysona.sona.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sona")
public record SonaProperties(Cors cors, RateLimit rateLimit, Share share) {

    public SonaProperties {
        if (cors == null) {
            cors = new Cors(List.of("http://localhost:5173", "https://trysona.io"));
        }
        if (rateLimit == null) {
            rateLimit = new RateLimit(10);
        }
        if (share == null) {
            share = new Share(7);
        }
    }

    public record Cors(List<String> allowedOrigins) {
    }

    public record RateLimit(int authRequestsPerSecond) {
    }

    public record Share(int expiryDays) {
    }
}
