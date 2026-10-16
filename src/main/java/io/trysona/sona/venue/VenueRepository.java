package io.trysona.sona.venue;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VenueRepository extends JpaRepository<Venue, Long> {

    Optional<Venue> findByUuid(UUID uuid);

    /**
     * Venues the user owns or can see through a team. {@code name} is a case-insensitive
     * substring filter; pass an empty string to match everything.
     */
    @Query("""
            select v from Venue v
            where (v.owner.id = :userId
                   or v.team.id in (select m.team.id from TeamMember m where m.user.id = :userId))
              and lower(v.name) like lower(concat('%', :name, '%'))
            """)
    Page<Venue> findAccessible(@Param("userId") Long userId, @Param("name") String name, Pageable pageable);
}
