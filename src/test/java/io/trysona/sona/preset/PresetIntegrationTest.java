package io.trysona.sona.preset;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.trysona.sona.support.IntegrationTest;
import org.junit.jupiter.api.Test;

class PresetIntegrationTest extends IntegrationTest {

    @Test
    void crudAndTabFilter() throws Exception {
        TestUser user = registerUser();
        String preset = read(postAs(user, "/api/presets", """
                {"name":"Arena sub","calculatorTab":"arrays","inputs":{"frequency":63,"temperature":24}}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.isPublic").value(false))
                .andExpect(jsonPath("$.data.inputs.frequency").value(63)), "$.data.uuid");
        postAs(user, "/api/presets", """
                {"name":"Room","calculatorTab":"acoustics","inputs":{"volume":5000}}""")
                .andExpect(status().isCreated());

        getAs(user, "/api/presets").andExpect(jsonPath("$.data.length()").value(2));
        getAs(user, "/api/presets?tab=arrays")
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Arena sub"));

        patchAs(user, "/api/presets/{uuid}", """
                {"name":"Arena sub v2","isPublic":true}""", preset)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Arena sub v2"))
                .andExpect(jsonPath("$.data.isPublic").value(true))
                .andExpect(jsonPath("$.data.inputs.temperature").value(24));
        patchAs(user, "/api/presets/{uuid}", "{}", preset).andExpect(status().isOk());

        deleteAs(user, "/api/presets/{uuid}", preset).andExpect(status().isOk());
        getAs(user, "/api/presets/{uuid}", preset).andExpect(status().isNotFound());
    }

    @Test
    void validatesTabAndInputs() throws Exception {
        TestUser user = registerUser();
        postAs(user, "/api/presets", """
                {"name":"x","calculatorTab":"Not A Tab","inputs":{}}""").andExpect(status().isBadRequest());
        postAs(user, "/api/presets", """
                {"name":"x","calculatorTab":"acoustics"}""").andExpect(status().isBadRequest());
    }

    @Test
    void privatePresetsAreHiddenFromStrangersButPublicOnesAreReadable() throws Exception {
        TestUser owner = registerUser();
        TestUser stranger = registerUser();
        String priv = read(postAs(owner, "/api/presets", """
                {"name":"secret","calculatorTab":"rf-wireless","inputs":{}}"""), "$.data.uuid");
        String pub = read(postAs(owner, "/api/presets", """
                {"name":"shared","calculatorTab":"rf-wireless","inputs":{},"isPublic":true}"""), "$.data.uuid");

        getAs(stranger, "/api/presets/{uuid}", priv).andExpect(status().isForbidden());
        getAs(stranger, "/api/presets/{uuid}", pub).andExpect(status().isOk());
        patchAs(stranger, "/api/presets/{uuid}", "{\"name\":\"mine\"}", pub).andExpect(status().isForbidden());
        deleteAs(stranger, "/api/presets/{uuid}", pub).andExpect(status().isForbidden());
        getAs(stranger, "/api/presets").andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void teamPresetsAreVisibleToMembers() throws Exception {
        TestUser owner = registerUser();
        TestUser viewer = registerUser();
        String team = createTeam(owner, "Preset crew");
        addMember(owner, team, viewer, "VIEWER");
        String preset = read(postAs(owner, "/api/presets", """
                {"name":"Team","calculatorTab":"gain-signal","inputs":{},"teamUuid":"%s"}""".formatted(team))
                .andExpect(jsonPath("$.data.teamUuid").value(team)), "$.data.uuid");

        getAs(viewer, "/api/presets?tab=gain-signal").andExpect(jsonPath("$.data.length()").value(1));
        getAs(viewer, "/api/presets/{uuid}", preset).andExpect(status().isOk());
    }
}
