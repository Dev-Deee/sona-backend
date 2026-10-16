package io.trysona.sona.venue;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.trysona.sona.auth.AuthUser;
import io.trysona.sona.common.ApiResponse;
import io.trysona.sona.common.PageResponse;
import io.trysona.sona.venue.dto.CreateVenueRequest;
import io.trysona.sona.venue.dto.UpdateVenueRequest;
import io.trysona.sona.venue.dto.VenueResponse;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/venues")
@Tag(name = "Venues")
public class VenueController {

    private final VenueService venueService;

    public VenueController(VenueService venueService) {
        this.venueService = venueService;
    }

    @GetMapping
    public ApiResponse<PageResponse<VenueResponse>> list(@AuthenticationPrincipal AuthUser auth,
                                                         @RequestParam(required = false) String name,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(venueService.list(auth.id(), name, page, size));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<VenueResponse> create(@AuthenticationPrincipal AuthUser auth,
                                             @Valid @RequestBody CreateVenueRequest request) {
        return ApiResponse.ok(venueService.create(auth.id(), request), "Venue created");
    }

    @GetMapping("/{uuid}")
    public ApiResponse<VenueResponse> get(@AuthenticationPrincipal AuthUser auth, @PathVariable UUID uuid) {
        return ApiResponse.ok(venueService.get(auth.id(), uuid));
    }

    @PatchMapping("/{uuid}")
    public ApiResponse<VenueResponse> update(@AuthenticationPrincipal AuthUser auth, @PathVariable UUID uuid,
                                             @Valid @RequestBody(required = false) UpdateVenueRequest request) {
        return ApiResponse.ok(venueService.update(auth.id(), uuid, request));
    }

    @DeleteMapping("/{uuid}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal AuthUser auth, @PathVariable UUID uuid) {
        venueService.delete(auth.id(), uuid);
        return ApiResponse.ok(null, "Venue deleted");
    }
}
