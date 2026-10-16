package io.trysona.sona.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.trysona.sona.auth.dto.AuthResponse;
import io.trysona.sona.auth.dto.LoginRequest;
import io.trysona.sona.auth.dto.RegisterRequest;
import io.trysona.sona.auth.jwt.JwtProperties;
import io.trysona.sona.auth.jwt.JwtService;
import io.trysona.sona.exception.DuplicateEmailException;
import io.trysona.sona.exception.ExpiredTokenException;
import io.trysona.sona.exception.InvalidCredentialsException;
import io.trysona.sona.exception.InvalidTokenException;
import io.trysona.sona.support.Fixtures;
import io.trysona.sona.user.User;
import io.trysona.sona.user.UserRepository;
import io.trysona.sona.user.UserRole;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-06T12:00:00Z");

    @Mock UserRepository userRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;

    AuthService service;

    @BeforeEach
    void setUp() {
        when(passwordEncoder.encode(anyString())).thenAnswer(inv -> "hashed:" + inv.getArgument(0));
        JwtProperties props = new JwtProperties("x".repeat(40), 900_000, 604_800_000);
        service = new AuthService(userRepository, refreshTokenRepository, passwordEncoder, jwtService, props,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void registerNormalisesEmailHashesPasswordAndDefaultsRole() {
        when(userRepository.existsByEmail("dj@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            u.setUuid(java.util.UUID.randomUUID());
            return u;
        });
        when(jwtService.generateAccessToken(any())).thenReturn("access");

        AuthResponse response = service.register(new RegisterRequest("  DJ@Example.com ", "password123", null));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("dj@example.com");
        assertThat(saved.getValue().getPassword()).isEqualTo("hashed:password123");
        assertThat(saved.getValue().getRole()).isEqualTo(UserRole.TOURING);
        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.refreshToken()).hasSizeGreaterThan(40);

        ArgumentCaptor<RefreshToken> refresh = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(refresh.capture());
        assertThat(refresh.getValue().getTokenHash()).isEqualTo(TokenHasher.sha256(response.refreshToken()));
        assertThat(refresh.getValue().getExpiresAt()).isEqualTo(NOW.plusMillis(604_800_000));
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);
        assertThatThrownBy(() -> service.register(new RegisterRequest("taken@example.com", "password123",
                UserRole.STUDENT))).isInstanceOf(DuplicateEmailException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void loginWithUnknownEmailStillRunsBcryptAndFails() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.login(new LoginRequest("ghost@example.com", "pw")))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(passwordEncoder).matches(eq("pw"), anyString());
    }

    @Test
    void loginWithWrongPasswordFails() {
        User user = Fixtures.user(1);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", user.getPassword())).thenReturn(false);
        assertThatThrownBy(() -> service.login(new LoginRequest(user.getEmail(), "wrong")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void loginSucceeds() {
        User user = Fixtures.user(1);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("right", user.getPassword())).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn("jwt");
        assertThat(service.login(new LoginRequest(user.getEmail(), "right")).accessToken()).isEqualTo("jwt");
    }

    @Test
    void refreshRotatesValidToken() {
        RefreshToken stored = stored(false, NOW.plusSeconds(60));
        when(refreshTokenRepository.findByTokenHash(TokenHasher.sha256("raw"))).thenReturn(Optional.of(stored));
        when(jwtService.generateAccessToken(stored.getUser())).thenReturn("new-jwt");

        AuthResponse response = service.refresh("raw");

        assertThat(stored.isRevoked()).isTrue();
        assertThat(response.accessToken()).isEqualTo("new-jwt");
        assertThat(response.refreshToken()).isNotEqualTo("raw");
    }

    @Test
    void refreshWithUnknownTokenFails() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.refresh("raw")).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void refreshWithRevokedTokenRevokesFamily() {
        RefreshToken stored = stored(true, NOW.plusSeconds(60));
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(stored));
        assertThatThrownBy(() -> service.refresh("raw")).isInstanceOf(InvalidTokenException.class);
        verify(refreshTokenRepository).revokeAllForUser(stored.getUser().getId());
    }

    @Test
    void refreshWithExpiredTokenFails() {
        RefreshToken stored = stored(false, NOW.minusSeconds(1));
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(stored));
        assertThatThrownBy(() -> service.refresh("raw")).isInstanceOf(ExpiredTokenException.class);
    }

    @Test
    void logoutRevokesAllRefreshTokens() {
        service.logout(7L);
        verify(refreshTokenRepository).revokeAllForUser(7L);
    }

    private static RefreshToken stored(boolean revoked, Instant expiresAt) {
        RefreshToken token = new RefreshToken();
        token.setUser(Fixtures.user(3));
        token.setRevoked(revoked);
        token.setExpiresAt(expiresAt);
        return token;
    }
}
