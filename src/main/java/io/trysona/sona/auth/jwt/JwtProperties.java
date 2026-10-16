package io.trysona.sona.auth.jwt;

import java.nio.charset.StandardCharsets;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(String secret, long accessExpiryMs, long refreshExpiryMs) {

    public JwtProperties {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("jwt.secret (JWT_SECRET) must be set and at least 256 bits (32 bytes) long");
        }
        if (accessExpiryMs <= 0 || refreshExpiryMs <= 0) {
            throw new IllegalStateException("jwt expiry values must be positive");
        }
    }

    @Override
    public String toString() {
        return "JwtProperties[accessExpiryMs=" + accessExpiryMs + ", refreshExpiryMs=" + refreshExpiryMs + "]";
    }
}
