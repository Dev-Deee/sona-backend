package io.trysona.sona.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.trysona.sona.support.IntegrationTest;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;

class SecurityIntegrationTest extends IntegrationTest {

    @Value("${jwt.secret}")
    String secret;

    @ParameterizedTest
    @CsvSource({
            "/api/users/me", "/api/venues", "/api/presets", "/api/teams",
            "/api/shows/00000000-0000-0000-0000-000000000000",
            "/api/venues/00000000-0000-0000-0000-000000000000/shows"})
    void protectedEndpointsReturn401WithoutToken(String url) throws Exception {
        mvc.perform(get(url))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    void tamperedSignatureIs401() throws Exception {
        TestUser user = registerUser();
        String[] parts = user.accessToken().split("\\.");
        String sig = parts[2];
        char flipped = sig.charAt(5) == 'A' ? 'B' : 'A';
        String tampered = parts[0] + "." + parts[1] + "." + sig.substring(0, 5) + flipped + sig.substring(6);
        mvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + tampered))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_TOKEN"));
    }

    @Test
    void tokenSignedWithDifferentKeyIs401() throws Exception {
        TestUser user = registerUser();
        String forged = Jwts.builder()
                .subject(user.uuid().toString())
                .claim("typ", "access")
                .expiration(Date.from(Instant.now().plusSeconds(600)))
                .signWith(Keys.hmacShaKeyFor("some-other-secret-key-that-is-long-enough!!".getBytes(StandardCharsets.UTF_8)))
                .compact();
        mvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + forged))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredTokenIs401WithTokenExpiredCode() throws Exception {
        TestUser user = registerUser();
        String expired = Jwts.builder()
                .subject(user.uuid().toString())
                .claim("typ", "access")
                .issuedAt(Date.from(Instant.now().minusSeconds(3600)))
                .expiration(Date.from(Instant.now().minusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();
        mvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + expired))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("TOKEN_EXPIRED"));
    }

    @Test
    void garbageTokenIs401() throws Exception {
        mvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_TOKEN"));
    }

    @Test
    void tokenOfDeletedUserIs401() throws Exception {
        TestUser user = registerUser();
        deleteAs(user, "/api/users/me").andExpect(status().isOk());
        getAs(user, "/api/users/me").andExpect(status().isUnauthorized());
    }

    @Test
    void corsPreflightAllowsDevOrigin() throws Exception {
        mvc.perform(options("/api/venues")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
    }

    @Test
    void corsPreflightRejectsUnknownOrigin() throws Exception {
        mvc.perform(options("/api/venues")
                        .header(HttpHeaders.ORIGIN, "https://evil.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden());
    }

    @Test
    void healthAndDocsArePublic() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Sona API"));
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
        mvc.perform(get("/api/docs")).andExpect(status().is3xxRedirection())
                .andExpect(header().string(HttpHeaders.LOCATION, "/swagger-ui/index.html"));
    }

    @Test
    void unknownRouteIs404Envelope() throws Exception {
        TestUser user = registerUser();
        getAs(user, "/api/does-not-exist")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }
}
