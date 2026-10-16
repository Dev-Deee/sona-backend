package io.trysona.sona.show;

import io.trysona.sona.common.Jsonb;
import io.trysona.sona.common.PageRequests;
import io.trysona.sona.common.PageResponse;
import io.trysona.sona.show.dto.CreateShowRequest;
import io.trysona.sona.show.dto.ShowResponse;
import io.trysona.sona.show.dto.UpdateShowRequest;
import io.trysona.sona.user.UserRepository;
import io.trysona.sona.venue.Venue;
import io.trysona.sona.venue.VenueService;
import jakarta.persistence.EntityNotFoundException;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Shows inherit their access rules from their venue. Deleting is limited to the show's creator
 * and the venue owner.
 */
@Service
public class ShowService {

    private final ShowRepository showRepository;
    private final VenueService venueService;
    private final UserRepository userRepository;

    public ShowService(ShowRepository showRepository, VenueService venueService, UserRepository userRepository) {
        this.showRepository = showRepository;
        this.venueService = venueService;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<ShowResponse> listForVenue(Long userId, UUID venueUuid, int page, int size) {
        var pageable = PageRequests.of(page, size,
                Sort.by(Sort.Order.desc("showDate").nullsLast(), Sort.Order.desc("createdAt")));
        Venue venue = venueService.loadReadable(venueUuid, userId);
        return PageResponse.of(showRepository.findByVenueId(venue.getId(), pageable), ShowResponse::from);
    }

    @Transactional
    public ShowResponse create(Long userId, UUID venueUuid, CreateShowRequest request) {
        Venue venue = venueService.loadWritable(venueUuid, userId);
        Show show = new Show();
        show.setName(request.name().trim());
        show.setShowDate(request.showDate());
        show.setVenue(venue);
        show.setOwner(userRepository.getReferenceById(userId));
        show.setCalculatorStates(Jsonb.objectOrEmpty(request.calculatorStates()));
        show.setInputList(Jsonb.arrayOrEmpty(request.inputList()));
        show.setNotes(request.notes());
        return ShowResponse.from(showRepository.saveAndFlush(show));
    }

    @Transactional(readOnly = true)
    public ShowResponse get(Long userId, UUID uuid) {
        Show show = load(uuid);
        venueService.loadReadable(show.getVenue().getUuid(), userId);
        return ShowResponse.from(show);
    }

    @Transactional
    public ShowResponse update(Long userId, UUID uuid, UpdateShowRequest request) {
        Show show = load(uuid);
        venueService.loadWritable(show.getVenue().getUuid(), userId);
        if (request != null) {
            if (request.name() != null) {
                show.setName(request.name().trim());
            }
            if (request.showDate() != null) {
                show.setShowDate(request.showDate());
            }
            if (request.calculatorStates() != null) {
                show.setCalculatorStates(Jsonb.merge(show.getCalculatorStates(), request.calculatorStates()));
            }
            if (request.inputList() != null) {
                show.setInputList(Jsonb.arrayOrEmpty(request.inputList()));
            }
            if (request.notes() != null) {
                show.setNotes(request.notes());
            }
        }
        return ShowResponse.from(showRepository.saveAndFlush(show));
    }

    @Transactional
    public void delete(Long userId, UUID uuid) {
        Show show = load(uuid);
        Venue venue = venueService.loadReadable(show.getVenue().getUuid(), userId);
        boolean showOwner = show.getOwner() != null && show.getOwner().getId().equals(userId);
        if (!showOwner && !venueService.isOwner(venue, userId)) {
            throw new AccessDeniedException("Only the show creator or venue owner can delete this show");
        }
        showRepository.delete(show);
    }

    private Show load(UUID uuid) {
        return showRepository.findByUuid(uuid).orElseThrow(() -> new EntityNotFoundException("Show not found"));
    }
}
