package io.trysona.sona.team;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.trysona.sona.exception.BadRequestException;
import io.trysona.sona.exception.ConflictException;
import io.trysona.sona.support.Fixtures;
import io.trysona.sona.team.dto.AddMemberRequest;
import io.trysona.sona.team.dto.UpdateMemberRequest;
import io.trysona.sona.user.User;
import io.trysona.sona.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TeamServiceTest {

    @Mock TeamRepository teamRepository;
    @Mock TeamMemberRepository memberRepository;
    @Mock UserRepository userRepository;
    @InjectMocks TeamService service;

    User owner = Fixtures.user(1);
    User editor = Fixtures.user(2);
    User outsider = Fixtures.user(3);
    Team team = Fixtures.team(10, owner);

    @BeforeEach
    void setUp() {
        when(teamRepository.findByUuid(team.getUuid())).thenReturn(Optional.of(team));
        when(memberRepository.find(10L, 1L)).thenReturn(Optional.of(Fixtures.member(team, owner, TeamRole.OWNER)));
        when(memberRepository.find(10L, 2L)).thenReturn(Optional.of(Fixtures.member(team, editor, TeamRole.EDITOR)));
        when(memberRepository.find(10L, 3L)).thenReturn(Optional.empty());
        when(userRepository.findByUuid(editor.getUuid())).thenReturn(Optional.of(editor));
        when(userRepository.findByUuid(owner.getUuid())).thenReturn(Optional.of(owner));
    }

    @Test
    void listIncludesMyRole() {
        when(memberRepository.findAllByUserId(2L)).thenReturn(List.of(Fixtures.member(team, editor, TeamRole.EDITOR)));
        when(teamRepository.findAllForMember(2L)).thenReturn(List.of(team));
        assertThat(service.list(2L)).singleElement().satisfies(t -> {
            assertThat(t.myRole()).isEqualTo(TeamRole.EDITOR);
            assertThat(t.ownerUuid()).isEqualTo(owner.getUuid());
        });
    }

    @Test
    void getRequiresMembership() {
        assertThatThrownBy(() -> service.get(3L, team.getUuid())).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void unknownTeamIs404() {
        when(teamRepository.findByUuid(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(1L, java.util.UUID.randomUUID()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void onlyOwnerAddsMembers() {
        assertThatThrownBy(() -> service.addMember(2L, team.getUuid(),
                new AddMemberRequest(outsider.getEmail(), TeamRole.VIEWER)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void addMemberRejectsOwnerRoleUnknownEmailAndDuplicates() {
        assertThatThrownBy(() -> service.addMember(1L, team.getUuid(),
                new AddMemberRequest(outsider.getEmail(), TeamRole.OWNER))).isInstanceOf(BadRequestException.class);

        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.addMember(1L, team.getUuid(),
                new AddMemberRequest("ghost@example.com", TeamRole.VIEWER)))
                .isInstanceOf(EntityNotFoundException.class);

        when(userRepository.findByEmail(editor.getEmail())).thenReturn(Optional.of(editor));
        assertThatThrownBy(() -> service.addMember(1L, team.getUuid(),
                new AddMemberRequest(editor.getEmail(), TeamRole.VIEWER))).isInstanceOf(ConflictException.class);
    }

    @Test
    void addMemberSaves() {
        when(userRepository.findByEmail(outsider.getEmail())).thenReturn(Optional.of(outsider));
        when(memberRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        var response = service.addMember(1L, team.getUuid(),
                new AddMemberRequest(" " + outsider.getEmail().toUpperCase() + " ", TeamRole.VIEWER));
        assertThat(response.userUuid()).isEqualTo(outsider.getUuid());
    }

    @Test
    void updateMemberCannotTouchOwner() {
        assertThatThrownBy(() -> service.updateMember(1L, team.getUuid(), owner.getUuid(),
                new UpdateMemberRequest(TeamRole.VIEWER))).isInstanceOf(BadRequestException.class);
        assertThat(service.updateMember(1L, team.getUuid(), editor.getUuid(),
                new UpdateMemberRequest(TeamRole.VIEWER)).role()).isEqualTo(TeamRole.VIEWER);
    }

    @Test
    void removeMemberRules() {
        assertThatThrownBy(() -> service.removeMember(1L, team.getUuid(), owner.getUuid()))
                .isInstanceOf(BadRequestException.class);
        service.removeMember(2L, team.getUuid(), editor.getUuid());
        verify(memberRepository).delete(any());
        assertThatThrownBy(() -> service.removeMember(2L, team.getUuid(), owner.getUuid()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void removeUnknownMemberIs404() {
        when(userRepository.findByUuid(outsider.getUuid())).thenReturn(Optional.of(outsider));
        assertThatThrownBy(() -> service.removeMember(1L, team.getUuid(), outsider.getUuid()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void onlyOwnerDeletesTeam() {
        assertThatThrownBy(() -> service.delete(2L, team.getUuid())).isInstanceOf(AccessDeniedException.class);
        verify(teamRepository, never()).deleteInBulk(any());
        service.delete(1L, team.getUuid());
        verify(teamRepository).deleteInBulk(10L);
    }

    @Test
    void loadEditableTeamRejectsViewers() {
        User viewer = Fixtures.user(4);
        when(memberRepository.find(10L, 4L)).thenReturn(Optional.of(Fixtures.member(team, viewer, TeamRole.VIEWER)));
        assertThatThrownBy(() -> service.loadEditableTeam(team.getUuid(), 4L))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(service.loadEditableTeam(team.getUuid(), 2L)).isSameAs(team);
    }
}
