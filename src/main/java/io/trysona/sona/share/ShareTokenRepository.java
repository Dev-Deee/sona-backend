package io.trysona.sona.share;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShareTokenRepository extends JpaRepository<ShareToken, Long> {

    Optional<ShareToken> findByToken(String token);

    boolean existsByToken(String token);

    @Modifying
    @Query("delete from ShareToken s where s.expiresAt < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") Instant cutoff);
}
