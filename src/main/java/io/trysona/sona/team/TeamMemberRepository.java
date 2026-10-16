package io.trysona.sona.team;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeamMemberRepository extends JpaRepository<TeamMember, TeamMemberId> {

    @Query("select m from TeamMember m join fetch m.user where m.team.id = :teamId order by m.joinedAt")
    List<TeamMember> findAllByTeamId(@Param("teamId") Long teamId);

    @Query("select m from TeamMember m join fetch m.team where m.user.id = :userId")
    List<TeamMember> findAllByUserId(@Param("userId") Long userId);

    default Optional<TeamMember> find(Long teamId, Long userId) {
        return findById(new TeamMemberId(teamId, userId));
    }
}
