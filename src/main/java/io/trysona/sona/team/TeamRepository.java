package io.trysona.sona.team;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeamRepository extends JpaRepository<Team, Long> {

    Optional<Team> findByUuid(UUID uuid);

    @Query("select m.team from TeamMember m where m.user.id = :userId order by m.team.createdAt desc")
    List<Team> findAllForMember(@Param("userId") Long userId);

    /**
     * Bulk delete so the database's ON DELETE rules (members cascade, team venues/presets detach)
     * apply without Hibernate tripping over already-loaded TeamMember rows.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Team t where t.id = :id")
    void deleteInBulk(@Param("id") Long id);
}
