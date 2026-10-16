package io.trysona.sona.venue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.trysona.sona.exception.BadRequestException;
import io.trysona.sona.exception.ConflictException;
import io.trysona.sona.show.ShowRepository;
import io.trysona.sona.support.Fixtures;
import io.trysona.sona.team.Team;
import io.trysona.sona.team.TeamRole;
import io.trysona.sona.team.TeamService;
import io.trysona.sona.user.User;
import io.trysona.sona.user.UserRepository;
import io.trysona.sona.venue.dto.CreateVenueRequest;
import io.trysona.sona.venue.dto.UpdateVenueRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class VenueServiceTest {

    @Mock VenueRepository venueRepository;
    @Mock ShowRepository showRepository;
    @Mock UserRepository userRepository;
    @Mock TeamService teamService;
    @InjectMocks VenueService service;

    User owner = Fixtures.user(1);
    Team team = Fixtures.team(10, owner);

    @Test
    void listPassesTrimmedFilterAndValidatesPaging() {
        Venue venue = Fixtures.venue(5, owner, null);
        when(venueRepository.findAccessible(eq(1L), eq("eko"), any()))
                .thenReturn(new PageImpl<>(List.of(venue), PageRequest.of(0, 20), 1));
        assertThat(service.list(1L, "  eko ", 0, 20).content()).hasSize(1);
        assertThatThrownBy(() -> service.list(1L, null, -1, 20)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void createAttachesTeamOnlyWhenRequested() {
        when(userRepository.getReferenceById(1L)).thenReturn(owner);
        when(venueRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(teamService.loadEditableTeam(team.getUuid(), 1L)).thenReturn(team);

        var personal = service.create(1L, new CreateVenueRequest(" Hall ", null, Map.of("length", 10), null, null));
        assertThat(personal.name()).isEqualTo("Hall");
        assertThat(personal.teamUuid()).isNull();

        var shared = service.create(1L, new CreateVenueRequest("Hall", team.getUuid(), null, null, "n"));
        assertThat(shared.teamUuid()).isEqualTo(team.getUuid());
    }

    @Test
    void readAccessForOwnerAndTeamMembersOnly() {
        Venue venue = Fixtures.venue(5, owner, team);
        when(venueRepository.findByUuid(venue.getUuid())).thenReturn(Optional.of(venue));
        when(teamService.roleOf(10L, 2L)).thenReturn(Optional.of(TeamRole.VIEWER));
        when(teamService.roleOf(10L, 3L)).thenReturn(Optional.empty());

        assertThat(service.get(1L, venue.getUuid()).uuid()).isEqualTo(venue.getUuid());
        assertThat(service.get(2L, venue.getUuid()).uuid()).isEqualTo(venue.getUuid());
        assertThatThrownBy(() -> service.get(3L, venue.getUuid())).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.loadWritable(venue.getUuid(), 2L)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void updateAppliesOnlyProvidedFields() {
        Venue venue = Fixtures.venue(5, owner, null);
        venue.setNotes("keep");
        when(venueRepository.findByUuid(venue.getUuid())).thenReturn(Optional.of(venue));
        when(venueRepository.saveAndFlush(venue)).thenReturn(venue);
        when(teamService.loadEditableTeam(team.getUuid(), 1L)).thenReturn(team);

        var result = service.update(1L, venue.getUuid(), new UpdateVenueRequest(" New ", team.getUuid(),
                Map.of("width", 3), Map.of("floor", 0.1), null));
        assertThat(result.name()).isEqualTo("New");
        assertThat(result.notes()).isEqualTo("keep");
        assertThat(result.teamUuid()).isEqualTo(team.getUuid());
        assertThat(service.update(1L, venue.getUuid(), null).name()).isEqualTo("New");
        assertThat(service.update(1L, venue.getUuid(), new UpdateVenueRequest(null, null, null, null, "x"))
                .notes()).isEqualTo("x");
    }

    @Test
    void deleteRequiresOwnershipAndNoShows() {
        Venue venue = Fixtures.venue(5, owner, team);
        when(venueRepository.findByUuid(venue.getUuid())).thenReturn(Optional.of(venue));
        when(teamService.roleOf(10L, 2L)).thenReturn(Optional.of(TeamRole.EDITOR));

        assertThatThrownBy(() -> service.delete(2L, venue.getUuid())).isInstanceOf(AccessDeniedException.class);

        when(showRepository.existsByVenueId(5L)).thenReturn(true);
        assertThatThrownBy(() -> service.delete(1L, venue.getUuid())).isInstanceOf(ConflictException.class);
        verify(venueRepository, never()).delete(any());

        when(showRepository.existsByVenueId(5L)).thenReturn(false);
        service.delete(1L, venue.getUuid());
        verify(venueRepository).delete(venue);
    }
}
