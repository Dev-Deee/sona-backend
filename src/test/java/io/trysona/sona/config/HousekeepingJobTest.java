package io.trysona.sona.config;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.trysona.sona.auth.RefreshTokenRepository;
import io.trysona.sona.share.ShareTokenRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HousekeepingJobTest {

    @Mock ShareTokenRepository shareTokens;
    @Mock RefreshTokenRepository refreshTokens;

    @Test
    void purgesExpiredRows() {
        Instant now = Instant.parse("2026-10-06T12:00:00Z");
        when(shareTokens.deleteExpiredBefore(now.minus(Duration.ofDays(1)))).thenReturn(2);
        when(refreshTokens.deleteExpiredBefore(now)).thenReturn(0);
        new HousekeepingJob(shareTokens, refreshTokens, Clock.fixed(now, ZoneOffset.UTC)).purgeExpired();
        verify(shareTokens).deleteExpiredBefore(now.minus(Duration.ofDays(1)));
        verify(refreshTokens).deleteExpiredBefore(now);
    }
}
