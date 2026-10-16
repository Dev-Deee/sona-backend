package io.trysona.sona.show;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShowRepository extends JpaRepository<Show, Long> {

    Optional<Show> findByUuid(UUID uuid);

    Page<Show> findByVenueId(Long venueId, Pageable pageable);

    boolean existsByVenueId(Long venueId);
}
