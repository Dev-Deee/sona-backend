package io.trysona.sona.team;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.trysona.sona.auth.AuthUser;
import io.trysona.sona.common.ApiResponse;
import io.trysona.sona.team.dto.AddMemberRequest;
import io.trysona.sona.team.dto.CreateTeamRequest;
import io.trysona.sona.team.dto.TeamMemberResponse;
import io.trysona.sona.team.dto.TeamResponse;
import io.trysona.sona.team.dto.UpdateMemberRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teams")
@Tag(name = "Teams")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @GetMapping
    public ApiResponse<List<TeamResponse>> list(@AuthenticationPrincipal AuthUser auth) {
        return ApiResponse.ok(teamService.list(auth.id()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TeamResponse> create(@AuthenticationPrincipal AuthUser auth,
                                            @Valid @RequestBody CreateTeamRequest request) {
        return ApiResponse.ok(teamService.create(auth.id(), request), "Team created");
    }

    @GetMapping("/{uuid}")
    public ApiResponse<TeamResponse> get(@AuthenticationPrincipal AuthUser auth, @PathVariable UUID uuid) {
        return ApiResponse.ok(teamService.get(auth.id(), uuid));
    }

    @PostMapping("/{uuid}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TeamMemberResponse> addMember(@AuthenticationPrincipal AuthUser auth, @PathVariable UUID uuid,
                                                     @Valid @RequestBody AddMemberRequest request) {
        return ApiResponse.ok(teamService.addMember(auth.id(), uuid, request), "Member added");
    }

    @PatchMapping("/{uuid}/members/{userUuid}")
    public ApiResponse<TeamMemberResponse> updateMember(@AuthenticationPrincipal AuthUser auth,
                                                        @PathVariable UUID uuid, @PathVariable UUID userUuid,
                                                        @Valid @RequestBody UpdateMemberRequest request) {
        return ApiResponse.ok(teamService.updateMember(auth.id(), uuid, userUuid, request));
    }

    @DeleteMapping("/{uuid}/members/{userUuid}")
    public ApiResponse<Void> removeMember(@AuthenticationPrincipal AuthUser auth, @PathVariable UUID uuid,
                                          @PathVariable UUID userUuid) {
        teamService.removeMember(auth.id(), uuid, userUuid);
        return ApiResponse.ok(null, "Member removed");
    }

    @DeleteMapping("/{uuid}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal AuthUser auth, @PathVariable UUID uuid) {
        teamService.delete(auth.id(), uuid);
        return ApiResponse.ok(null, "Team deleted");
    }
}
