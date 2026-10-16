package io.trysona.sona.support;

import io.trysona.sona.preset.Preset;
import io.trysona.sona.show.Show;
import io.trysona.sona.team.Team;
import io.trysona.sona.team.TeamMember;
import io.trysona.sona.team.TeamRole;
import io.trysona.sona.user.User;
import io.trysona.sona.venue.Venue;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.UUID;

/** Builders for detached entities used by unit tests. */
public final class Fixtures {

    private Fixtures() {
    }

    public static User user(long id) {
        User user = new User();
        user.setId(id);
        user.setUuid(UUID.randomUUID());
        user.setEmail("user" + id + "@example.com");
        user.setPassword("$2a$hash");
        user.setPreferences(new LinkedHashMap<>());
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        return user;
    }

    public static Team team(long id, User owner) {
        Team team = new Team();
        team.setId(id);
        team.setUuid(UUID.randomUUID());
        team.setName("Team " + id);
        team.setCreatedBy(owner);
        team.setCreatedAt(Instant.now());
        return team;
    }

    public static TeamMember member(Team team, User user, TeamRole role) {
        TeamMember member = new TeamMember(team, user, role);
        member.setJoinedAt(Instant.now());
        return member;
    }

    public static Venue venue(long id, User owner, Team team) {
        Venue venue = new Venue();
        venue.setId(id);
        venue.setUuid(UUID.randomUUID());
        venue.setName("Venue " + id);
        venue.setOwner(owner);
        venue.setTeam(team);
        return venue;
    }

    public static Show show(long id, Venue venue, User owner) {
        Show show = new Show();
        show.setId(id);
        show.setUuid(UUID.randomUUID());
        show.setName("Show " + id);
        show.setVenue(venue);
        show.setOwner(owner);
        return show;
    }

    public static Preset preset(long id, User owner, Team team, boolean isPublic) {
        Preset preset = new Preset();
        preset.setId(id);
        preset.setUuid(UUID.randomUUID());
        preset.setName("Preset " + id);
        preset.setCalculatorTab("acoustics");
        preset.setOwner(owner);
        preset.setTeam(team);
        preset.setPublicPreset(isPublic);
        return preset;
    }
}
