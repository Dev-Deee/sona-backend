package io.trysona.sona.show;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.trysona.sona.show.dto.CreateShowRequest;
import io.trysona.sona.show.dto.UpdateShowRequest;
import io.trysona.sona.support.Fixtures;
import io.trysona.sona.user.User;
import io.trysona.sona.user.UserRepository;
import io.trysona.sona.venue.Venue;
import io.trysona.sona.venue.VenueService;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class ShowServiceTest {

    @Mock ShowRepository showRepository;
    @Mock VenueService venueService;
    @Mock UserRepository userRepository;
    @InjectMocks ShowService service;

    User owner = Fixtures.user(1);
    Venue venue = Fixtures.venue(5, owner, null);

    @Test
    void listUsesReadableVenue() {
        when(venueService.loadReadable(venue.getUuid(), 1L)).thenReturn(venue);
        when(showRepository.findByVenueId(eq(5L), any()))
                .thenReturn(new PageImpl<>(List.of(Fixtures.show(9, venue, owner)), PageRequest.of(0, 20), 1));
        assertThat(service.listForVenue(1L, venue.getUuid(), 0, 20).totalElements()).isEqualTo(1);
    }

    @Test
    void createDefaultsJsonb() {
        when(venueService.loadWritable(venue.getUuid(), 1L)).thenReturn(venue);
        when(userRepository.getReferenceById(1L)).thenReturn(owner);
        when(showRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        var show = service.create(1L, venue.getUuid(), new CreateShowRequest(" Gig ", null, null, null, null));
        assertThat(show.name()).isEqualTo("Gig");
        assertThat(show.calculatorStates()).isEmpty();
        assertThat(show.inputList()).isEmpty();
    }

    @Test
    void updateMergesCalculatorStates() {
        Show show = Fixtures.show(9, venue, owner);
        show.setCalculatorStates(new java.util.HashMap<>(Map.of("acoustics", Map.of("f", 80))));
        when(showRepository.findByUuid(show.getUuid())).thenReturn(Optional.of(show));
        when(venueService.loadWritable(venue.getUuid(), 1L)).thenReturn(venue);
        when(showRepository.saveAndFlush(show)).thenReturn(show);

        var result = service.update(1L, show.getUuid(), new UpdateShowRequest("New", LocalDate.of(2026, 1, 1),
                Map.of("coverage", Map.of("a", 1)), List.of(Map.of("ch", 1)), "notes"));
        assertThat(result.calculatorStates()).containsKeys("acoustics", "coverage");
        assertThat(result.inputList()).hasSize(1);
        assertThat(service.update(1L, show.getUuid(), null).name()).isEqualTo("New");
    }

    @Test
    void getChecksVenueAccessAndMissingShowIs404() {
        Show show = Fixtures.show(9, venue, owner);
        when(showRepository.findByUuid(show.getUuid())).thenReturn(Optional.of(show));
        assertThat(service.get(1L, show.getUuid()).venueUuid()).isEqualTo(venue.getUuid());
        verify(venueService).loadReadable(venue.getUuid(), 1L);

        when(showRepository.findByUuid(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(1L, UUID.randomUUID())).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void deleteAllowedForCreatorOrVenueOwnerOnly() {
        User creator = Fixtures.user(2);
        Show show = Fixtures.show(9, venue, creator);
        when(showRepository.findByUuid(show.getUuid())).thenReturn(Optional.of(show));
        when(venueService.loadReadable(eq(venue.getUuid()), any())).thenReturn(venue);
        when(venueService.isOwner(venue, 3L)).thenReturn(false);
        when(venueService.isOwner(venue, 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(3L, show.getUuid())).isInstanceOf(AccessDeniedException.class);
        service.delete(2L, show.getUuid());
        service.delete(1L, show.getUuid());
        verify(showRepository, org.mockito.Mockito.times(2)).delete(show);
    }
}
