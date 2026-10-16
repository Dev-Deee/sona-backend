package io.trysona.sona.show;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.trysona.sona.support.IntegrationTest;
import org.junit.jupiter.api.Test;

class ShowIntegrationTest extends IntegrationTest {

    @Test
    void createListGetUpdateDelete() throws Exception {
        TestUser user = registerUser();
        String venue = createVenue(user, "Show venue");
        String show = read(postAs(user, "/api/venues/{uuid}/shows", """
                {"name":"Friday night","showDate":"2026-11-20",
                 "calculatorStates":{"acoustics":{"frequency":80}},
                 "inputList":[{"channel":1,"source":"Kick In","phantom":false}]}""", venue)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.venueUuid").value(venue))
                .andExpect(jsonPath("$.data.ownerUuid").value(user.uuid().toString()))
                .andExpect(jsonPath("$.data.inputList[0].source").value("Kick In")), "$.data.uuid");

        getAs(user, "/api/venues/{uuid}/shows", venue)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));

        patchAs(user, "/api/shows/{uuid}", """
                {"calculatorStates":{"coverage":{"angle":90}}}""", show)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.calculatorStates.acoustics.frequency").value(80))
                .andExpect(jsonPath("$.data.calculatorStates.coverage.angle").value(90))
                .andExpect(jsonPath("$.data.showDate").value("2026-11-20"));

        patchAs(user, "/api/shows/{uuid}", "{}", show).andExpect(status().isOk());
        getAs(user, "/api/shows/{uuid}", show).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Friday night"));

        deleteAs(user, "/api/shows/{uuid}", show).andExpect(status().isOk());
        getAs(user, "/api/shows/{uuid}", show).andExpect(status().isNotFound());
    }

    @Test
    void defaultsEmptyJsonbWhenOmitted() throws Exception {
        TestUser user = registerUser();
        String venue = createVenue(user, "Defaults");
        postAs(user, "/api/venues/{uuid}/shows", "{\"name\":\"Bare\",\"calculatorStates\":null}", venue)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.calculatorStates").isMap())
                .andExpect(jsonPath("$.data.inputList").isArray());
    }

    @Test
    void strangersCannotSeeOrModifyShows() throws Exception {
        TestUser owner = registerUser();
        TestUser stranger = registerUser();
        String venue = createVenue(owner, "Mine");
        String show = read(postAs(owner, "/api/venues/{uuid}/shows", "{\"name\":\"Mine\"}", venue), "$.data.uuid");

        getAs(stranger, "/api/venues/{uuid}/shows", venue).andExpect(status().isForbidden());
        postAs(stranger, "/api/venues/{uuid}/shows", "{\"name\":\"x\"}", venue).andExpect(status().isForbidden());
        getAs(stranger, "/api/shows/{uuid}", show).andExpect(status().isForbidden());
        patchAs(stranger, "/api/shows/{uuid}", "{\"name\":\"x\"}", show).andExpect(status().isForbidden());
        deleteAs(stranger, "/api/shows/{uuid}", show).andExpect(status().isForbidden());
    }

    @Test
    void teamEditorCanCreateButOnlyCreatorOrVenueOwnerCanDelete() throws Exception {
        TestUser owner = registerUser();
        TestUser editorA = registerUser();
        TestUser editorB = registerUser();
        String team = createTeam(owner, "Shows crew");
        addMember(owner, team, editorA, "EDITOR");
        addMember(owner, team, editorB, "EDITOR");
        String venue = read(postAs(owner, "/api/venues", """
                {"name":"Shared","teamUuid":"%s"}""".formatted(team)), "$.data.uuid");

        String show = read(postAs(editorA, "/api/venues/{uuid}/shows", "{\"name\":\"A's show\"}", venue)
                .andExpect(status().isCreated()), "$.data.uuid");
        deleteAs(editorB, "/api/shows/{uuid}", show).andExpect(status().isForbidden());
        deleteAs(owner, "/api/shows/{uuid}", show).andExpect(status().isOk());
    }

    @Test
    void createValidatesName() throws Exception {
        TestUser user = registerUser();
        String venue = createVenue(user, "V");
        postAs(user, "/api/venues/{uuid}/shows", "{\"name\":\"\"}", venue).andExpect(status().isBadRequest());
    }
}
