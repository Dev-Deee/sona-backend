package io.trysona.sona.config;

import io.trysona.sona.auth.RefreshTokenRepository;
import io.trysona.sona.share.ShareTokenRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Purges expired share links and refresh tokens once an hour.
 */
@Component
public class HousekeepingJob {

    private static final Logger log = LoggerFactory.getLogger(HousekeepingJob.class);

    private final ShareTokenRepository shareTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final Clock clock;

    public HousekeepingJob(ShareTokenRepository shareTokenRepository,
                           RefreshTokenRepository refreshTokenRepository, Clock clock) {
        this.shareTokenRepository = shareTokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "PT1H", initialDelayString = "PT5M")
    @Transactional
    public void purgeExpired() {
        // Keep expired share links for a day so recipients get a 410 rather than a 404.
        Instant now = clock.instant();
        int shares = shareTokenRepository.deleteExpiredBefore(now.minus(Duration.ofDays(1)));
        int refresh = refreshTokenRepository.deleteExpiredBefore(now);
        if (shares > 0 || refresh > 0) {
            log.info("Housekeeping removed {} share tokens and {} refresh tokens", shares, refresh);
        }
    }
}
