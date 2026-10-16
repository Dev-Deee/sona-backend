package io.trysona.sona.preset;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PresetRepository extends JpaRepository<Preset, Long> {

    Optional<Preset> findByUuid(UUID uuid);

    /**
     * Presets the user owns or can see through a team. Pass an empty {@code tab} to match all tabs.
     */
    @Query("""
            select p from Preset p
            where (p.owner.id = :userId
                   or p.team.id in (select m.team.id from TeamMember m where m.user.id = :userId))
              and (:tab = '' or p.calculatorTab = :tab)
            order by p.createdAt desc
            """)
    List<Preset> findAccessible(@Param("userId") Long userId, @Param("tab") String tab);
}
