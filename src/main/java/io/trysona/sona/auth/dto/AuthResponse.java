package io.trysona.sona.auth.dto;

import io.trysona.sona.user.dto.UserResponse;

public record AuthResponse(String accessToken, String refreshToken, String tokenType, long expiresIn,
                           UserResponse user) {

    @Override
    public String toString() {
        return "AuthResponse[user=" + user.uuid() + "]";
    }
}
