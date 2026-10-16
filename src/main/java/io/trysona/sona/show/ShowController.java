package io.trysona.sona.show;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.trysona.sona.auth.AuthUser;
import io.trysona.sona.common.ApiResponse;
import io.trysona.sona.common.PageResponse;
import io.trysona.sona.show.dto.CreateShowRequest;
import io.trysona.sona.show.dto.ShowResponse;
import io.trysona.sona.show.dto.UpdateShowRequest;
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
@RequestMapping("/api")
@Tag(name = "Shows")
public class ShowController {

    private final ShowService showService;

    public ShowController(ShowService showService) {
        this.showService = showService;
    }

    @GetMapping("/venues/{venueUuid}/shows")
    public ApiResponse<PageResponse<ShowResponse>> list(@AuthenticationPrincipal AuthUser auth,
                                                        @PathVariable UUID venueUuid,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(showService.listForVenue(auth.id(), venueUuid, page, size));
    }

    @PostMapping("/venues/{venueUuid}/shows")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ShowResponse> create(@AuthenticationPrincipal AuthUser auth, @PathVariable UUID venueUuid,
                                            @Valid @RequestBody CreateShowRequest request) {
        return ApiResponse.ok(showService.create(auth.id(), venueUuid, request), "Show created");
    }

    @GetMapping("/shows/{uuid}")
    public ApiResponse<ShowResponse> get(@AuthenticationPrincipal AuthUser auth, @PathVariable UUID uuid) {
        return ApiResponse.ok(showService.get(auth.id(), uuid));
    }

    @PatchMapping("/shows/{uuid}")
    public ApiResponse<ShowResponse> update(@AuthenticationPrincipal AuthUser auth, @PathVariable UUID uuid,
                                            @Valid @RequestBody(required = false) UpdateShowRequest request) {
        return ApiResponse.ok(showService.update(auth.id(), uuid, request));
    }

    @DeleteMapping("/shows/{uuid}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal AuthUser auth, @PathVariable UUID uuid) {
        showService.delete(auth.id(), uuid);
        return ApiResponse.ok(null, "Show deleted");
    }
}
