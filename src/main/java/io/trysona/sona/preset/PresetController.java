package io.trysona.sona.preset;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.trysona.sona.auth.AuthUser;
import io.trysona.sona.common.ApiResponse;
import io.trysona.sona.preset.dto.CreatePresetRequest;
import io.trysona.sona.preset.dto.PresetResponse;
import io.trysona.sona.preset.dto.UpdatePresetRequest;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/presets")
@Tag(name = "Presets")
public class PresetController {

    private final PresetService presetService;

    public PresetController(PresetService presetService) {
        this.presetService = presetService;
    }

    @GetMapping
    public ApiResponse<List<PresetResponse>> list(@AuthenticationPrincipal AuthUser auth,
                                                  @RequestParam(required = false) String tab) {
        return ApiResponse.ok(presetService.list(auth.id(), tab));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PresetResponse> create(@AuthenticationPrincipal AuthUser auth,
                                              @Valid @RequestBody CreatePresetRequest request) {
        return ApiResponse.ok(presetService.create(auth.id(), request), "Preset saved");
    }

    @GetMapping("/{uuid}")
    public ApiResponse<PresetResponse> get(@AuthenticationPrincipal AuthUser auth, @PathVariable UUID uuid) {
        return ApiResponse.ok(presetService.get(auth.id(), uuid));
    }

    @PatchMapping("/{uuid}")
    public ApiResponse<PresetResponse> update(@AuthenticationPrincipal AuthUser auth, @PathVariable UUID uuid,
                                              @Valid @RequestBody(required = false) UpdatePresetRequest request) {
        return ApiResponse.ok(presetService.update(auth.id(), uuid, request));
    }

    @DeleteMapping("/{uuid}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal AuthUser auth, @PathVariable UUID uuid) {
        presetService.delete(auth.id(), uuid);
        return ApiResponse.ok(null, "Preset deleted");
    }
}
