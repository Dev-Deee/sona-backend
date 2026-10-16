package io.trysona.sona.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.trysona.sona.auth.jwt.JwtProperties;
import io.trysona.sona.auth.jwt.JwtService;
import io.trysona.sona.exception.ExpiredTokenException;
import io.trysona.sona.exception.InvalidTokenException;
import io.trysona.sona.support.Fixtures;
import io.trysona.sona.user.User;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-key-that-is-at-least-32-bytes";
    private static final Instant NOW = Instant.parse("2026-10-06T12:00:00Z");
    private final JwtProperties props = new JwtProperties(SECRET, 900_000, 604_800_000);

    @Test
    void roundTripsUserUuid() {
        JwtService service = new JwtService(props, Clock.fixed(NOW, ZoneOffset.UTC));
        User user = Fixtures.user(1);
        assertThat(service.validateAccessToken(service.generateAccessToken(user))).isEqualTo(user.getUuid());
        assertThat(service.accessExpirySeconds()).isEqualTo(900);
    }

    @Test
    void tokenExpiresAfterFifteenMinutes() {
        User user = Fixtures.user(1);
        String token = new JwtService(props, Clock.fixed(NOW, ZoneOffset.UTC)).generateAccessToken(user);
        JwtService later = new JwtService(props, Clock.fixed(NOW.plusSeconds(901), ZoneOffset.UTC));
        assertThatThrownBy(() -> later.validateAccessToken(token)).isInstanceOf(ExpiredTokenException.class);
    }

    @Test
    void rejectsTokenWithoutAccessType() {
        String token = Jwts.builder().subject(java.util.UUID.randomUUID().toString())
                .expiration(Date.from(NOW.plusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
        JwtService service = new JwtService(props, Clock.fixed(NOW, ZoneOffset.UTC));
        assertThatThrownBy(() -> service.validateAccessToken(token)).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void rejectsNonUuidSubject() {
        String token = Jwts.builder().subject("not-a-uuid").claim("typ", "access")
                .expiration(Date.from(NOW.plusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
        JwtService service = new JwtService(props, Clock.fixed(NOW, ZoneOffset.UTC));
        assertThatThrownBy(() -> service.validateAccessToken(token)).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void rejectsGarbage() {
        JwtService service = new JwtService(props, Clock.fixed(NOW, ZoneOffset.UTC));
        assertThatThrownBy(() -> service.validateAccessToken("")).isInstanceOf(InvalidTokenException.class);
        assertThatThrownBy(() -> service.validateAccessToken("a.b.c")).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void propertiesRejectShortSecretAndNonPositiveExpiry() {
        assertThatThrownBy(() -> new JwtProperties("short", 1, 1)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new JwtProperties(null, 1, 1)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new JwtProperties(SECRET, 0, 1)).isInstanceOf(IllegalStateException.class);
        assertThat(props.toString()).doesNotContain(SECRET);
    }
}
