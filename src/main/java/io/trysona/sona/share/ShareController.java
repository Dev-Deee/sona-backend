package io.trysona.sona.share;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.trysona.sona.auth.AuthUser;
import io.trysona.sona.common.ApiResponse;
import io.trysona.sona.share.dto.CreateShareRequest;
import io.trysona.sona.share.dto.ShareCreatedResponse;
import io.trysona.sona.share.dto.SharePayloadResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/share")
@Tag(name = "Share links")
public class ShareController {

    private final ShareService shareService;

    public ShareController(ShareService shareService) {
        this.shareService = shareService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a 7-day share link for a calculator result")
    public ApiResponse<ShareCreatedResponse> create(@AuthenticationPrincipal AuthUser auth,
                                                    @Valid @RequestBody CreateShareRequest request) {
        return ApiResponse.ok(shareService.create(auth.id(), request), "Share link created");
    }

    @GetMapping("/{token}")
    @Operation(summary = "Open a share link (public)")
    public ApiResponse<SharePayloadResponse> get(@PathVariable String token) {
        return ApiResponse.ok(shareService.resolve(token));
    }
}
