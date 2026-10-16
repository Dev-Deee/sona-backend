package io.trysona.sona.team.dto;

import io.trysona.sona.team.TeamMember;
import io.trysona.sona.team.TeamRole;
import java.time.Instant;
import java.util.UUID;

public record TeamMemberResponse(UUID userUuid, String email, TeamRole role, Instant joinedAt) {

    public static TeamMemberResponse from(TeamMember member) {
        return new TeamMemberResponse(member.getUser().getUuid(), member.getUser().getEmail(), member.getRole(),
                member.getJoinedAt());
    }
}
