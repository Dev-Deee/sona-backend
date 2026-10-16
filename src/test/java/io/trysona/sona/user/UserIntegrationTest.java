package io.trysona.sona.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.trysona.sona.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class UserIntegrationTest extends IntegrationTest {

    @Test
    void getMeReturnsProfile() throws Exception {
        TestUser user = registerUser();
        getAs(user, "/api/users/me")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.uuid").value(user.uuid().toString()))
                .andExpect(jsonPath("$.data.email").value(user.email()))
                .andExpect(jsonPath("$.data.preferences").isMap());
    }

    @Test
    void patchWithEmptyObjectIsNoOp() throws Exception {
        TestUser user = registerUser();
        patchAs(user, "/api/users/me", "{}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("TOURING"));
    }

    @Test
    void patchWithNoBodyIsNoOp() throws Exception {
        TestUser user = registerUser();
        patchAs(user, "/api/users/me", null).andExpect(status().isOk());
    }

    @Test
    void patchUpdatesRoleAndMergesPreferencesWithNullRemovingKeys() throws Exception {
        TestUser user = registerUser();
        patchAs(user, "/api/users/me", """
                {"role":"HOUSE_OF_WORSHIP","preferences":{"units":"imperial","theme":"dim","nested":{"a":null}}}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("HOUSE_OF_WORSHIP"))
                .andExpect(jsonPath("$.data.preferences.units").value("imperial"))
                .andExpect(jsonPath("$.data.preferences.nested.a").doesNotExist());
        patchAs(user, "/api/users/me", """
                {"preferences":{"theme":null}}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.preferences.units").value("imperial"))
                .andExpect(jsonPath("$.data.preferences.theme").doesNotExist());
    }

    @Test
    void patchWithInvalidRoleIs400() throws Exception {
        TestUser user = registerUser();
        patchAs(user, "/api/users/me", """
                {"role":"ROCKSTAR"}""").andExpect(status().isBadRequest());
    }

    @Test
    void changePasswordRequiresCurrentPasswordAndRevokesRefreshTokens() throws Exception {
        TestUser user = registerUser();
        patchAs(user, "/api/users/me/password", """
                {"currentPassword":"nope-nope-nope","newPassword":"brand-new-password"}""")
                .andExpect(status().isBadRequest());
        patchAs(user, "/api/users/me/password", """
                {"currentPassword":"%s","newPassword":"brand-new-password"}""".formatted(user.password()))
                .andExpect(status().isOk());

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email":"%s","password":"brand-new-password"}""".formatted(user.email())))
                .andExpect(status().isOk());
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(user.refreshToken())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteMeCascadesOwnedData() throws Exception {
        TestUser user = registerUser();
        String venue = createVenue(user, "Doomed Hall");
        postAs(user, "/api/venues/{uuid}/shows", """
                {"name":"Last show"}""", venue).andExpect(status().isCreated());
        createTeam(user, "Doomed Crew");
        postAs(user, "/api/presets", """
                {"name":"p","calculatorTab":"acoustics","inputs":{}}""").andExpect(status().isCreated());

        deleteAs(user, "/api/users/me").andExpect(status().isOk());

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email":"%s","password":"%s"}""".formatted(user.email(), user.password())))
                .andExpect(status().isUnauthorized());
    }
}
