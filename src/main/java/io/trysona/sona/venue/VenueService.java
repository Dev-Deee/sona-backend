package io.trysona.sona.venue;

import io.trysona.sona.common.PageRequests;
import io.trysona.sona.common.PageResponse;
import io.trysona.sona.exception.ConflictException;
import io.trysona.sona.show.ShowRepository;
import io.trysona.sona.team.TeamRole;
import io.trysona.sona.team.TeamService;
import io.trysona.sona.user.UserRepository;
import io.trysona.sona.venue.dto.CreateVenueRequest;
import io.trysona.sona.venue.dto.UpdateVenueRequest;
import io.trysona.sona.venue.dto.VenueResponse;
import jakarta.persistence.EntityNotFoundException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Venues are visible to their owner and to members of the venue's team. Team EDITORs and OWNERs can
 * modify a team venue; only the venue owner can delete it.
 */
@Service
public class VenueService {

    private final VenueRepository venueRepository;
    private final ShowRepository showRepository;
    private final UserRepository userRepository;
    private final TeamService teamService;

    public VenueService(VenueRepository venueRepository, ShowRepository showRepository,
                        UserRepository userRepository, TeamService teamService) {
        this.venueRepository = venueRepository;
        this.showRepository = showRepository;
        this.userRepository = userRepository;
        this.teamService = teamService;
    }

    @Transactional(readOnly = true)
    public PageResponse<VenueResponse> list(Long userId, String name, int page, int size) {
        var pageable = PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        String filter = name == null ? "" : name.trim();
        return PageResponse.of(venueRepository.findAccessible(userId, filter, pageable), VenueResponse::from);
    }

    @Transactional
    public VenueResponse create(Long userId, CreateVenueRequest request) {
        Venue venue = new Venue();
        venue.setName(request.name().trim());
        venue.setOwner(userRepository.getReferenceById(userId));
        if (request.teamUuid() != null) {
            venue.setTeam(teamService.loadEditableTeam(request.teamUuid(), userId));
        }
        venue.setDimensions(request.dimensions());
        venue.setSurfaces(request.surfaces());
        venue.setNotes(request.notes());
        return VenueResponse.from(venueRepository.saveAndFlush(venue));
    }

    @Transactional(readOnly = true)
    public VenueResponse get(Long userId, UUID uuid) {
        return VenueResponse.from(loadReadable(uuid, userId));
    }

    @Transactional
    public VenueResponse update(Long userId, UUID uuid, UpdateVenueRequest request) {
        Venue venue = loadWritable(uuid, userId);
        if (request != null) {
            if (request.name() != null) {
                venue.setName(request.name().trim());
            }
            if (request.teamUuid() != null) {
                venue.setTeam(teamService.loadEditableTeam(request.teamUuid(), userId));
            }
            if (request.dimensions() != null) {
                venue.setDimensions(request.dimensions());
            }
            if (request.surfaces() != null) {
                venue.setSurfaces(request.surfaces());
            }
            if (request.notes() != null) {
                venue.setNotes(request.notes());
            }
        }
        return VenueResponse.from(venueRepository.saveAndFlush(venue));
    }

    @Transactional
    public void delete(Long userId, UUID uuid) {
        Venue venue = loadReadable(uuid, userId);
        if (!isOwner(venue, userId)) {
            throw new AccessDeniedException("Only the venue owner can delete it");
        }
        if (showRepository.existsByVenueId(venue.getId())) {
            throw new ConflictException("Venue has shows; delete or move them before deleting the venue");
        }
        venueRepository.delete(venue);
    }

    /** Loads a venue the user may view, or throws 404/403. */
    @Transactional(readOnly = true)
    public Venue loadReadable(UUID uuid, Long userId) {
        Venue venue = load(uuid);
        if (!isOwner(venue, userId) && teamRole(venue, userId).isEmpty()) {
            throw new AccessDeniedException("You do not have access to this venue");
        }
        return venue;
    }

    /** Loads a venue the user may modify, or throws 404/403. */
    @Transactional(readOnly = true)
    public Venue loadWritable(UUID uuid, Long userId) {
        Venue venue = loadReadable(uuid, userId);
        if (!isOwner(venue, userId) && !teamRole(venue, userId).map(TeamRole::canEdit).orElse(false)) {
            throw new AccessDeniedException("You have read-only access to this venue");
        }
        return venue;
    }

    public boolean isOwner(Venue venue, Long userId) {
        return venue.getOwner() != null && venue.getOwner().getId().equals(userId);
    }

    private Optional<TeamRole> teamRole(Venue venue, Long userId) {
        return venue.getTeam() == null ? Optional.empty() : teamService.roleOf(venue.getTeam().getId(), userId);
    }

    private Venue load(UUID uuid) {
        return venueRepository.findByUuid(uuid).orElseThrow(() -> new EntityNotFoundException("Venue not found"));
    }
}
