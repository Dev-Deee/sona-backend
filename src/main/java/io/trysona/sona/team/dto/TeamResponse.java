package io.trysona.sona.team.dto;

import io.trysona.sona.team.Team;
import io.trysona.sona.team.TeamRole;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * {@code members} is populated on the single-team endpoint and empty in list responses.
 */
public record TeamResponse(UUID uuid, String name, UUID ownerUuid, TeamRole myRole, Instant createdAt,
                           List<TeamMemberResponse> members) {

    public static TeamResponse from(Team team, TeamRole myRole, List<TeamMemberResponse> members) {
        UUID owner = team.getCreatedBy() == null ? null : team.getCreatedBy().getUuid();
        return new TeamResponse(team.getUuid(), team.getName(), owner, myRole, team.getCreatedAt(), members);
    }
}
