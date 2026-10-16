package io.trysona.sona.share;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.trysona.sona.support.IntegrationTest;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class ShareIntegrationTest extends IntegrationTest {

    @Autowired
    ShareTokenRepository repository;

    @Test
    void createdLinkIsPubliclyReadableAndMultiUseByDefault() throws Exception {
        TestUser user = registerUser();
        String token = read(postAs(user, "/api/share", """
                {"calculatorTab":"acoustics","inputs":{"frequency":80,"temperature":21.6},
                 "outputs":{"wavelength":4.306}}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.singleUse").value(false))
                .andExpect(jsonPath("$.data.expiresAt").exists()), "$.data.token");

        for (int i = 0; i < 2; i++) {
            mvc.perform(get("/api/share/{token}", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.calculatorTab").value("acoustics"))
                    .andExpect(jsonPath("$.data.inputs.frequency").value(80))
                    .andExpect(jsonPath("$.data.outputs.wavelength").value(4.306));
        }
    }

    @Test
    void singleUseLinkWorksOnceThenIs410() throws Exception {
        TestUser user = registerUser();
        String token = read(postAs(user, "/api/share", """
                {"calculatorTab":"coverage","inputs":{},"singleUse":true}"""), "$.data.token");
        mvc.perform(get("/api/share/{token}", token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.outputs").isMap());
        mvc.perform(get("/api/share/{token}", token)).andExpect(status().isGone());
    }

    @Test
    void expiredLinkIs410() throws Exception {
        TestUser user = registerUser();
        String token = read(postAs(user, "/api/share", """
                {"calculatorTab":"arrays","inputs":{}}"""), "$.data.token");
        ShareToken share = repository.findByToken(token).orElseThrow();
        share.setExpiresAt(Instant.now().minusSeconds(5));
        repository.saveAndFlush(share);

        mvc.perform(get("/api/share/{token}", token))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.error").value("GONE"));
    }

    @Test
    void unknownTokenIs404() throws Exception {
        mvc.perform(get("/api/share/{token}", "doesnotexist")).andExpect(status().isNotFound());
    }

    @Test
    void creatingRequiresAuthAndValidPayload() throws Exception {
        mvc.perform(post("/api/share").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"calculatorTab\":\"acoustics\",\"inputs\":{}}"))
                .andExpect(status().isUnauthorized());
        TestUser user = registerUser();
        postAs(user, "/api/share", "{\"inputs\":{}}").andExpect(status().isBadRequest());
    }
}
