package io.trysona.sona.share;

import io.trysona.sona.common.Jsonb;
import io.trysona.sona.config.SonaProperties;
import io.trysona.sona.exception.GoneException;
import io.trysona.sona.share.dto.CreateShareRequest;
import io.trysona.sona.share.dto.ShareCreatedResponse;
import io.trysona.sona.share.dto.SharePayloadResponse;
import io.trysona.sona.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShareService {

    static final int TOKEN_LENGTH = 12;
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ShareTokenRepository repository;
    private final UserRepository userRepository;
    private final SonaProperties properties;
    private final Clock clock;

    public ShareService(ShareTokenRepository repository, UserRepository userRepository, SonaProperties properties,
                        Clock clock) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public ShareCreatedResponse create(Long userId, CreateShareRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("calculatorTab", request.calculatorTab());
        payload.put("inputs", Jsonb.objectOrEmpty(request.inputs()));
        payload.put("outputs", Jsonb.objectOrEmpty(request.outputs()));

        ShareToken share = new ShareToken();
        share.setToken(uniqueToken());
        share.setPayload(payload);
        share.setCreatedBy(userRepository.getReferenceById(userId));
        share.setSingleUse(Boolean.TRUE.equals(request.singleUse()));
        share.setExpiresAt(clock.instant().plus(Duration.ofDays(properties.share().expiryDays())));
        repository.save(share);
        return new ShareCreatedResponse(share.getToken(), share.getExpiresAt(), share.isSingleUse());
    }

    /**
     * @throws EntityNotFoundException if no such token exists (404)
     * @throws GoneException           if it has expired or a single-use link was already opened (410)
     */
    @Transactional
    @SuppressWarnings("unchecked")
    public SharePayloadResponse resolve(String token) {
        ShareToken share = repository.findByToken(token)
                .orElseThrow(() -> new EntityNotFoundException("Share link not found"));
        Instant now = clock.instant();
        if (share.getExpiresAt().isBefore(now)) {
            throw new GoneException("This share link has expired");
        }
        if (share.isSingleUse()) {
            if (share.getUsedAt() != null) {
                throw new GoneException("This share link has already been used");
            }
            share.setUsedAt(now);
        }
        Map<String, Object> payload = share.getPayload();
        return new SharePayloadResponse(
                (String) payload.get("calculatorTab"),
                (Map<String, Object>) payload.getOrDefault("inputs", Map.of()),
                (Map<String, Object>) payload.getOrDefault("outputs", Map.of()),
                share.getExpiresAt());
    }

    private String uniqueToken() {
        String token;
        do {
            token = randomToken();
        } while (repository.existsByToken(token));
        return token;
    }

    static String randomToken() {
        StringBuilder sb = new StringBuilder(TOKEN_LENGTH);
        for (int i = 0; i < TOKEN_LENGTH; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
