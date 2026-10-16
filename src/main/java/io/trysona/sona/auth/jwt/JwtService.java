package io.trysona.sona.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.trysona.sona.exception.ExpiredTokenException;
import io.trysona.sona.exception.InvalidTokenException;
import io.trysona.sona.user.User;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/**
 * Issues and verifies short-lived HS256 access tokens. Refresh tokens are opaque and handled by
 * {@link io.trysona.sona.auth.AuthService}.
 */
@Service
public class JwtService {

    private static final String TYPE_CLAIM = "typ";
    private static final String ACCESS_TYPE = "access";

    private final SecretKey key;
    private final JwtProperties properties;
    private final Clock clock;

    public JwtService(JwtProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(User user) {
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(user.getUuid().toString())
                .claim("email", user.getEmail())
                .claim(TYPE_CLAIM, ACCESS_TYPE)
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(properties.accessExpiryMs())))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * @return the user UUID carried in the token's subject
     * @throws ExpiredTokenException if the token has expired
     * @throws InvalidTokenException if the token is malformed, tampered with, or not an access token
     */
    public UUID validateAccessToken(String token) {
        Claims claims;
        try {
            claims = Jwts.parser()
                    .verifyWith(key)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new ExpiredTokenException("Access token has expired");
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidTokenException("Access token is invalid");
        }
        if (!ACCESS_TYPE.equals(claims.get(TYPE_CLAIM, String.class))) {
            throw new InvalidTokenException("Access token is invalid");
        }
        try {
            return UUID.fromString(claims.getSubject());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new InvalidTokenException("Access token is invalid");
        }
    }

    public long accessExpirySeconds() {
        return properties.accessExpiryMs() / 1000;
    }
}
