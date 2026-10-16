package io.trysona.sona.share;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.trysona.sona.config.SonaProperties;
import io.trysona.sona.exception.GoneException;
import io.trysona.sona.share.dto.CreateShareRequest;
import io.trysona.sona.support.Fixtures;
import io.trysona.sona.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShareServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-06T12:00:00Z");

    @Mock ShareTokenRepository repository;
    @Mock UserRepository userRepository;
    ShareService service;

    @BeforeEach
    void setUp() {
        service = new ShareService(repository, userRepository, new SonaProperties(null, null, null),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createRetriesOnCollisionAndExpiresInSevenDays() {
        when(repository.existsByToken(anyString())).thenReturn(true, false);
        when(userRepository.getReferenceById(1L)).thenReturn(Fixtures.user(1));

        var created = service.create(1L, new CreateShareRequest("acoustics", Map.of("f", 80), null, null));

        ArgumentCaptor<ShareToken> saved = ArgumentCaptor.forClass(ShareToken.class);
        verify(repository).save(saved.capture());
        assertThat(created.token()).hasSize(ShareService.TOKEN_LENGTH).isEqualTo(saved.getValue().getToken());
        assertThat(created.expiresAt()).isEqualTo(NOW.plus(Duration.ofDays(7)));
        assertThat(created.singleUse()).isFalse();
        assertThat(saved.getValue().getPayload()).containsKeys("calculatorTab", "inputs", "outputs");
    }

    @Test
    void resolveHandlesMissingExpiredAndSingleUse() {
        when(repository.findByToken("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.resolve("missing")).isInstanceOf(EntityNotFoundException.class);

        ShareToken expired = token(NOW.minusSeconds(1), false);
        when(repository.findByToken("expired")).thenReturn(Optional.of(expired));
        assertThatThrownBy(() -> service.resolve("expired")).isInstanceOf(GoneException.class);

        ShareToken once = token(NOW.plusSeconds(60), true);
        when(repository.findByToken("once")).thenReturn(Optional.of(once));
        assertThat(service.resolve("once").calculatorTab()).isEqualTo("rf-wireless");
        assertThat(once.getUsedAt()).isEqualTo(NOW);
        assertThatThrownBy(() -> service.resolve("once")).isInstanceOf(GoneException.class);
    }

    @Test
    void randomTokensUseUnambiguousAlphabet() {
        for (int i = 0; i < 200; i++) {
            assertThat(ShareService.randomToken()).matches("[A-HJ-NP-Za-km-z2-9]{12}");
        }
    }

    private static ShareToken token(Instant expiresAt, boolean singleUse) {
        ShareToken token = new ShareToken();
        token.setExpiresAt(expiresAt);
        token.setSingleUse(singleUse);
        token.setPayload(Map.of("calculatorTab", "rf-wireless", "inputs", Map.of(), "outputs", Map.of()));
        return token;
    }
}
