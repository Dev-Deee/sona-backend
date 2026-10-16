package io.trysona.sona.user;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.trysona.sona.auth.AuthUser;
import io.trysona.sona.common.ApiResponse;
import io.trysona.sona.user.dto.ChangePasswordRequest;
import io.trysona.sona.user.dto.UpdateUserRequest;
import io.trysona.sona.user.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me")
@Tag(name = "Users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ApiResponse<UserResponse> me(@AuthenticationPrincipal AuthUser auth) {
        return ApiResponse.ok(userService.getMe(auth.id()));
    }

    @PatchMapping
    public ApiResponse<UserResponse> update(@AuthenticationPrincipal AuthUser auth,
                                            @Valid @RequestBody(required = false) UpdateUserRequest request) {
        return ApiResponse.ok(userService.update(auth.id(), request));
    }

    @PatchMapping("/password")
    public ApiResponse<Void> changePassword(@AuthenticationPrincipal AuthUser auth,
                                            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(auth.id(), request);
        return ApiResponse.ok(null, "Password updated");
    }

    @DeleteMapping
    public ApiResponse<Void> delete(@AuthenticationPrincipal AuthUser auth) {
        userService.delete(auth.id());
        return ApiResponse.ok(null, "Account deleted");
    }
}
