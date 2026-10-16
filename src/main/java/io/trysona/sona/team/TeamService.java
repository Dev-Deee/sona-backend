package io.trysona.sona.team;

import io.trysona.sona.exception.BadRequestException;
import io.trysona.sona.exception.ConflictException;
import io.trysona.sona.team.dto.AddMemberRequest;
import io.trysona.sona.team.dto.CreateTeamRequest;
import io.trysona.sona.team.dto.TeamMemberResponse;
import io.trysona.sona.team.dto.TeamResponse;
import io.trysona.sona.team.dto.UpdateMemberRequest;
import io.trysona.sona.user.User;
import io.trysona.sona.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository memberRepository;
    private final UserRepository userRepository;

    public TeamService(TeamRepository teamRepository, TeamMemberRepository memberRepository,
                       UserRepository userRepository) {
        this.teamRepository = teamRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> list(Long userId) {
        Map<Long, TeamRole> roles = memberRepository.findAllByUserId(userId).stream()
                .collect(Collectors.toMap(m -> m.getTeam().getId(), TeamMember::getRole));
        return teamRepository.findAllForMember(userId).stream()
                .map(team -> TeamResponse.from(team, roles.get(team.getId()), List.of()))
                .toList();
    }

    @Transactional
    public TeamResponse create(Long userId, CreateTeamRequest request) {
        User creator = userRepository.getReferenceById(userId);
        Team team = new Team();
        team.setName(request.name().trim());
        team.setCreatedBy(creator);
        team = teamRepository.save(team);
        memberRepository.save(new TeamMember(team, creator, TeamRole.OWNER));
        return get(userId, team.getUuid());
    }

    @Transactional(readOnly = true)
    public TeamResponse get(Long userId, UUID teamUuid) {
        Team team = loadTeam(teamUuid);
        TeamRole myRole = requireMember(team, userId);
        List<TeamMemberResponse> members = memberRepository.findAllByTeamId(team.getId()).stream()
                .map(TeamMemberResponse::from)
                .toList();
        return TeamResponse.from(team, myRole, members);
    }

    @Transactional
    public TeamMemberResponse addMember(Long userId, UUID teamUuid, AddMemberRequest request) {
        Team team = loadTeam(teamUuid);
        requireOwner(team, userId);
        rejectOwnerRole(request.role());
        User invitee = userRepository.findByEmail(request.email().trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new EntityNotFoundException("No Sona account exists for that email"));
        if (memberRepository.find(team.getId(), invitee.getId()).isPresent()) {
            throw new ConflictException("User is already a member of this team");
        }
        TeamMember member = memberRepository.saveAndFlush(new TeamMember(team, invitee, request.role()));
        return TeamMemberResponse.from(member);
    }

    @Transactional
    public TeamMemberResponse updateMember(Long userId, UUID teamUuid, UUID memberUuid, UpdateMemberRequest request) {
        Team team = loadTeam(teamUuid);
        requireOwner(team, userId);
        rejectOwnerRole(request.role());
        TeamMember member = loadMember(team, memberUuid);
        if (member.getRole() == TeamRole.OWNER) {
            throw new BadRequestException("The team owner's role cannot be changed");
        }
        member.setRole(request.role());
        return TeamMemberResponse.from(member);
    }

    /**
     * The owner can remove anyone except themselves; any member can remove themselves (leave).
     */
    @Transactional
    public void removeMember(Long userId, UUID teamUuid, UUID memberUuid) {
        Team team = loadTeam(teamUuid);
        TeamRole myRole = requireMember(team, userId);
        TeamMember member = loadMember(team, memberUuid);
        boolean self = member.getUser().getId().equals(userId);
        if (!self && myRole != TeamRole.OWNER) {
            throw new AccessDeniedException("Only the team owner can remove members");
        }
        if (member.getRole() == TeamRole.OWNER) {
            throw new BadRequestException("The team owner cannot be removed; delete the team instead");
        }
        memberRepository.delete(member);
    }

    @Transactional
    public void delete(Long userId, UUID teamUuid) {
        Team team = loadTeam(teamUuid);
        requireOwner(team, userId);
        teamRepository.deleteInBulk(team.getId());
    }

    /** The caller's role in the team, if they are a member. */
    @Transactional(readOnly = true)
    public Optional<TeamRole> roleOf(Long teamId, Long userId) {
        return memberRepository.find(teamId, userId).map(TeamMember::getRole);
    }

    /**
     * Resolves a team the caller may attach resources (venues, presets) to.
     */
    @Transactional(readOnly = true)
    public Team loadEditableTeam(UUID teamUuid, Long userId) {
        Team team = loadTeam(teamUuid);
        TeamRole role = requireMember(team, userId);
        if (!role.canEdit()) {
            throw new AccessDeniedException("Viewers cannot add resources to this team");
        }
        return team;
    }

    private Team loadTeam(UUID uuid) {
        return teamRepository.findByUuid(uuid).orElseThrow(() -> new EntityNotFoundException("Team not found"));
    }

    private TeamMember loadMember(Team team, UUID memberUuid) {
        User user = userRepository.findByUuid(memberUuid)
                .orElseThrow(() -> new EntityNotFoundException("Team member not found"));
        return memberRepository.find(team.getId(), user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Team member not found"));
    }

    private TeamRole requireMember(Team team, Long userId) {
        return memberRepository.find(team.getId(), userId)
                .map(TeamMember::getRole)
                .orElseThrow(() -> new AccessDeniedException("You are not a member of this team"));
    }

    private void requireOwner(Team team, Long userId) {
        if (requireMember(team, userId) != TeamRole.OWNER) {
            throw new AccessDeniedException("Only the team owner can do this");
        }
    }

    private static void rejectOwnerRole(TeamRole role) {
        if (role == TeamRole.OWNER) {
            throw new BadRequestException("A team has exactly one owner; assign EDITOR or VIEWER");
        }
    }
}
