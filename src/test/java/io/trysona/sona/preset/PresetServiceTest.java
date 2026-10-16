package io.trysona.sona.preset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.trysona.sona.preset.dto.CreatePresetRequest;
import io.trysona.sona.preset.dto.UpdatePresetRequest;
import io.trysona.sona.support.Fixtures;
import io.trysona.sona.team.Team;
import io.trysona.sona.team.TeamRole;
import io.trysona.sona.team.TeamService;
import io.trysona.sona.user.User;
import io.trysona.sona.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class PresetServiceTest {

    @Mock PresetRepository presetRepository;
    @Mock UserRepository userRepository;
    @Mock TeamService teamService;
    @InjectMocks PresetService service;

    User owner = Fixtures.user(1);
    Team team = Fixtures.team(10, owner);

    @Test
    void listNormalisesNullTab() {
        when(presetRepository.findAccessible(1L, "")).thenReturn(List.of(Fixtures.preset(1, owner, null, false)));
        assertThat(service.list(1L, null)).hasSize(1);
    }

    @Test
    void createWithTeamAndPublicFlag() {
        when(userRepository.getReferenceById(1L)).thenReturn(owner);
        when(teamService.loadEditableTeam(team.getUuid(), 1L)).thenReturn(team);
        when(presetRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        var preset = service.create(1L, new CreatePresetRequest(" P ", "arrays", Map.of("n", 4), team.getUuid(), true));
        assertThat(preset.name()).isEqualTo("P");
        assertThat(preset.isPublic()).isTrue();
        assertThat(preset.teamUuid()).isEqualTo(team.getUuid());
        assertThat(service.create(1L, new CreatePresetRequest("Q", "arrays", Map.of(), null, null)).isPublic())
                .isFalse();
    }

    @Test
    void getRules() {
        User stranger = Fixtures.user(3);
        Preset priv = Fixtures.preset(1, owner, null, false);
        Preset pub = Fixtures.preset(2, owner, null, true);
        Preset teamPreset = Fixtures.preset(3, owner, team, false);
        when(presetRepository.findByUuid(priv.getUuid())).thenReturn(Optional.of(priv));
        when(presetRepository.findByUuid(pub.getUuid())).thenReturn(Optional.of(pub));
        when(presetRepository.findByUuid(teamPreset.getUuid())).thenReturn(Optional.of(teamPreset));
        when(teamService.roleOf(10L, 3L)).thenReturn(Optional.of(TeamRole.VIEWER));

        assertThat(service.get(1L, priv.getUuid())).isNotNull();
        assertThatThrownBy(() -> service.get(stranger.getId(), priv.getUuid()))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(service.get(stranger.getId(), pub.getUuid())).isNotNull();
        assertThat(service.get(stranger.getId(), teamPreset.getUuid())).isNotNull();
    }

    @Test
    void updateAndDeleteOwnerOnly() {
        Preset preset = Fixtures.preset(1, owner, null, false);
        when(presetRepository.findByUuid(preset.getUuid())).thenReturn(Optional.of(preset));
        when(presetRepository.saveAndFlush(preset)).thenReturn(preset);

        var updated = service.update(1L, preset.getUuid(),
                new UpdatePresetRequest("Renamed", "coverage", Map.of("x", 1), true));
        assertThat(updated.calculatorTab()).isEqualTo("coverage");
        assertThat(updated.isPublic()).isTrue();
        assertThat(service.update(1L, preset.getUuid(), null).name()).isEqualTo("Renamed");

        assertThatThrownBy(() -> service.update(2L, preset.getUuid(), null)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.delete(2L, preset.getUuid())).isInstanceOf(AccessDeniedException.class);
        service.delete(1L, preset.getUuid());
        verify(presetRepository).delete(preset);
    }

    @Test
    void missingPresetIs404() {
        when(presetRepository.findByUuid(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(1L, UUID.randomUUID())).isInstanceOf(EntityNotFoundException.class);
    }
}
