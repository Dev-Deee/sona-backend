package io.trysona.sona.auth;

import io.trysona.sona.auth.dto.AuthResponse;
import io.trysona.sona.auth.dto.LoginRequest;
import io.trysona.sona.auth.dto.RegisterRequest;
import io.trysona.sona.auth.jwt.JwtProperties;
import io.trysona.sona.auth.jwt.JwtService;
import io.trysona.sona.exception.DuplicateEmailException;
import io.trysona.sona.exception.ExpiredTokenException;
import io.trysona.sona.exception.InvalidCredentialsException;
import io.trysona.sona.exception.InvalidTokenException;
import io.trysona.sona.user.User;
import io.trysona.sona.user.UserRepository;
import io.trysona.sona.user.UserRole;
import io.trysona.sona.user.dto.UserResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final Clock clock;
    /** Compared against when the email is unknown so login timing does not reveal which emails exist. */
    private final String dummyHash;

    public AuthService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder, JwtService jwtService, JwtProperties jwtProperties,
                       Clock clock) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.clock = clock;
        this.dummyHash = passwordEncoder.encode("timing-equaliser-not-a-real-password");
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normaliseEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException();
        }
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(request.role() == null ? UserRole.TOURING : request.role());
        user.setPreferences(new LinkedHashMap<>());
        user = userRepository.save(user);
        log.info("Registered user {}", user.getUuid());
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Optional<User> found = userRepository.findByEmail(normaliseEmail(request.email()));
        if (found.isEmpty()) {
            passwordEncoder.matches(request.password(), dummyHash);
            throw new InvalidCredentialsException("Invalid email or password");
        }
        User user = found.get();
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }
        return issueTokens(user);
    }

    /**
     * Rotates a refresh token: the presented token is revoked and a new pair is issued. Presenting
     * an already-revoked token (e.g. after logout, or a replayed token) fails with 401.
     */
    @Transactional(noRollbackFor = {InvalidTokenException.class, ExpiredTokenException.class})
    public AuthResponse refresh(String rawRefreshToken) {
        RefreshToken stored = refreshTokenRepository.findByTokenHash(TokenHasher.sha256(rawRefreshToken))
                .orElseThrow(() -> new InvalidTokenException("Refresh token is invalid"));
        if (stored.isRevoked()) {
            // Reuse of a rotated/revoked token suggests theft: revoke the whole family for this user.
            refreshTokenRepository.revokeAllForUser(stored.getUser().getId());
            throw new InvalidTokenException("Refresh token has been revoked");
        }
        if (stored.getExpiresAt().isBefore(clock.instant())) {
            throw new ExpiredTokenException("Refresh token has expired");
        }
        stored.setRevoked(true);
        return issueTokens(stored.getUser());
    }

    /**
     * Revokes every refresh token the user holds, signing them out on all devices. Access tokens
     * remain valid until they expire (at most 15 minutes).
     */
    @Transactional
    public void logout(Long userId) {
        refreshTokenRepository.revokeAllForUser(userId);
    }

    private AuthResponse issueTokens(User user) {
        String rawRefresh = TokenHasher.newOpaqueToken();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setTokenHash(TokenHasher.sha256(rawRefresh));
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(Instant.now(clock).plusMillis(jwtProperties.refreshExpiryMs()));
        refreshTokenRepository.save(refreshToken);
        return new AuthResponse(jwtService.generateAccessToken(user), rawRefresh, "Bearer",
                jwtService.accessExpirySeconds(), UserResponse.from(user));
    }

    static String normaliseEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
