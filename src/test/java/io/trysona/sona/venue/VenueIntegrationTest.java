package io.trysona.sona.venue;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.trysona.sona.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class VenueIntegrationTest extends IntegrationTest {

    @Test
    void createGetUpdateDeleteLifecycle() throws Exception {
        TestUser user = registerUser();
        String uuid = read(postAs(user, "/api/venues", """
                {"name":"  The Roundhouse ","dimensions":{"length":45,"width":45,"height":18,"unit":"m"},
                 "surfaces":{"floor":0.02,"ceiling":0.3},"notes":"Load in via rear dock"}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("The Roundhouse"))
                .andExpect(jsonPath("$.data.ownerUuid").value(user.uuid().toString()))
                .andExpect(jsonPath("$.data.id").doesNotExist())
                .andExpect(jsonPath("$.data.dimensions.length").value(45)), "$.data.uuid");

        getAs(user, "/api/venues/{uuid}", uuid)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.surfaces.ceiling").value(0.3));

        patchAs(user, "/api/venues/{uuid}", """
                {"notes":"Updated"}""", uuid)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.notes").value("Updated"))
                .andExpect(jsonPath("$.data.name").value("The Roundhouse"));

        deleteAs(user, "/api/venues/{uuid}", uuid).andExpect(status().isOk());
        getAs(user, "/api/venues/{uuid}", uuid).andExpect(status().isNotFound());
    }

    @Test
    void createRequiresName() throws Exception {
        TestUser user = registerUser();
        postAs(user, "/api/venues", "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("name"));
    }

    @Test
    void jsonbFieldsMayBeNullOrContainNulls() throws Exception {
        TestUser user = registerUser();
        String uuid = read(postAs(user, "/api/venues", """
                {"name":"Sparse","dimensions":{"length":null,"width":12,"unit":"m"},"surfaces":null}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.surfaces").isEmpty()), "$.data.uuid");
        getAs(user, "/api/venues/{uuid}", uuid)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dimensions.width").value(12));
    }

    @Test
    void patchWithEmptyBodyIsNoOp() throws Exception {
        TestUser user = registerUser();
        String uuid = createVenue(user, "Unchanged");
        patchAs(user, "/api/venues/{uuid}", "{}", uuid)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Unchanged"));
        patchAs(user, "/api/venues/{uuid}", null, uuid).andExpect(status().isOk());
    }

    @Test
    void patchRejectsBlankName() throws Exception {
        TestUser user = registerUser();
        String uuid = createVenue(user, "Named");
        patchAs(user, "/api/venues/{uuid}", """
                {"name":"   "}""", uuid).andExpect(status().isBadRequest());
    }

    @Test
    void accessingAnotherUsersVenueIs403() throws Exception {
        TestUser owner = registerUser();
        TestUser stranger = registerUser();
        String uuid = createVenue(owner, "Private");
        getAs(stranger, "/api/venues/{uuid}", uuid).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
        patchAs(stranger, "/api/venues/{uuid}", "{\"name\":\"Mine now\"}", uuid).andExpect(status().isForbidden());
        deleteAs(stranger, "/api/venues/{uuid}", uuid).andExpect(status().isForbidden());
    }

    @Test
    void unknownVenueIs404AndMalformedUuidIs400() throws Exception {
        TestUser user = registerUser();
        getAs(user, "/api/venues/{uuid}", "8f14e45f-ceea-4e7a-9f6b-000000000000").andExpect(status().isNotFound());
        getAs(user, "/api/venues/not-a-uuid").andExpect(status().isBadRequest());
    }

    @Test
    void deletingVenueWithShowsIs409() throws Exception {
        TestUser user = registerUser();
        String uuid = createVenue(user, "Busy");
        postAs(user, "/api/venues/{uuid}/shows", "{\"name\":\"Gig\"}", uuid).andExpect(status().isCreated());
        deleteAs(user, "/api/venues/{uuid}", uuid)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("has shows")));
    }

    @Test
    void listIsScopedPaginatedAndFilterable() throws Exception {
        TestUser user = registerUser();
        TestUser other = registerUser();
        createVenue(user, "O2 Academy");
        createVenue(user, "Eko Convention Centre");
        createVenue(user, "Academy Two");
        createVenue(other, "Someone else's Academy");

        getAs(user, "/api/venues?name=academy&size=10")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content.length()").value(2));
        getAs(user, "/api/venues?page=0&size=1")
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(3))
                .andExpect(jsonPath("$.data.content.length()").value(1));
    }

    @ParameterizedTest
    @CsvSource({"-1,20,page", "0,0,size", "0,1000,size"})
    void invalidPaginationIs400WithClearMessage(int page, int size, String mentions) throws Exception {
        TestUser user = registerUser();
        getAs(user, "/api/venues?page={p}&size={s}", page, size)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString(mentions)));
    }

    @Test
    void teamMembersSeeTeamVenuesAndOnlyEditorsCanModify() throws Exception {
        TestUser owner = registerUser();
        TestUser editor = registerUser();
        TestUser viewer = registerUser();
        String team = createTeam(owner, "Crew");
        addMember(owner, team, editor, "EDITOR");
        addMember(owner, team, viewer, "VIEWER");

        String venue = read(postAs(owner, "/api/venues", """
                {"name":"Team venue","teamUuid":"%s"}""".formatted(team))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.teamUuid").value(team)), "$.data.uuid");

        getAs(viewer, "/api/venues/{uuid}", venue).andExpect(status().isOk());
        getAs(viewer, "/api/venues").andExpect(jsonPath("$.data.totalElements").value(1));
        patchAs(viewer, "/api/venues/{uuid}", "{\"notes\":\"x\"}", venue).andExpect(status().isForbidden());
        patchAs(editor, "/api/venues/{uuid}", "{\"notes\":\"x\"}", venue).andExpect(status().isOk());
        deleteAs(editor, "/api/venues/{uuid}", venue).andExpect(status().isForbidden());

        // Viewers cannot attach their own venues to the team either.
        postAs(viewer, "/api/venues", """
                {"name":"Sneaky","teamUuid":"%s"}""".formatted(team)).andExpect(status().isForbidden());
    }
}
