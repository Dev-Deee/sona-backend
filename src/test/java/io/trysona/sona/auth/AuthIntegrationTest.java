package io.trysona.sona.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.trysona.sona.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class AuthIntegrationTest extends IntegrationTest {

    @Test
    void registerReturnsTokensAndUuidButNoInternalIdOrPassword() throws Exception {
        String email = "New.User+" + System.nanoTime() + "@Example.com";
        String body = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email":"%s","password":"long-enough-pw","role":"STUDENT"}""".formatted(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Account created"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(900))
                .andExpect(jsonPath("$.data.user.email").value(email.toLowerCase()))
                .andExpect(jsonPath("$.data.user.role").value("STUDENT"))
                .andExpect(jsonPath("$.data.user.uuid").exists())
                .andExpect(jsonPath("$.data.user.id").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("long-enough-pw").doesNotContain("password");
    }

    @Test
    void registerWithDuplicateEmailIs409() throws Exception {
        TestUser existing = registerUser();
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email":"%s","password":"another-password"}""".formatted(existing.email().toUpperCase())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("DUPLICATE_EMAIL"));
    }

    @Test
    void registerValidationErrorsAreFieldLevel() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email":"not-an-email","password":"short"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[*].field").value(hasItem("email")))
                .andExpect(jsonPath("$.details[*].field").value(hasItem("password")));
    }

    @Test
    void registerWithMissingBodyIs400() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"));
    }

    @Test
    void loginSucceedsWithCorrectPasswordAndIsCaseInsensitiveOnEmail() throws Exception {
        TestUser user = registerUser();
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email":"%s","password":"%s"}""".formatted(user.email().toUpperCase(), user.password())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists());
    }

    @Test
    void loginWithWrongPasswordIs401() throws Exception {
        TestUser user = registerUser();
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email":"%s","password":"wrong-password"}""".formatted(user.email())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"));
    }

    @Test
    void loginWithUnknownEmailIs401WithSameMessage() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email":"nobody@example.com","password":"whatever-password"}"""))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void refreshRotatesTokensAndOldRefreshTokenCannotBeReused() throws Exception {
        TestUser user = registerUser();
        String newRefresh = read(mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(user.refreshToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.refreshToken").value(not(user.refreshToken()))), "$.data.refreshToken");

        // Replaying the rotated token fails and revokes the whole family, including the new token.
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(user.refreshToken())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_TOKEN"));
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(newRefresh)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshTokenReuseAfterLogoutIs401() throws Exception {
        TestUser user = registerUser();
        postAs(user, "/api/auth/logout", "").andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out"));
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(user.refreshToken())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshWithUnknownTokenIs401() throws Exception {
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"made-up\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_TOKEN"));
    }

    @Test
    void logoutWithoutTokenIs401() throws Exception {
        mvc.perform(post("/api/auth/logout")).andExpect(status().isUnauthorized());
    }
}
