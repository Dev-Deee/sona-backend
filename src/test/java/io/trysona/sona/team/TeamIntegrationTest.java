package io.trysona.sona.team;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.trysona.sona.support.IntegrationTest;
import org.junit.jupiter.api.Test;

class TeamIntegrationTest extends IntegrationTest {

    @Test
    void creatorBecomesOwnerAndCanManageMembers() throws Exception {
        TestUser owner = registerUser();
        TestUser member = registerUser();
        String team = read(postAs(owner, "/api/teams", "{\"name\":\"Main stage\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.myRole").value("OWNER"))
                .andExpect(jsonPath("$.data.ownerUuid").value(owner.uuid().toString()))
                .andExpect(jsonPath("$.data.members.length()").value(1)), "$.data.uuid");

        postAs(owner, "/api/teams/{uuid}/members", """
                {"email":"%s","role":"VIEWER"}""".formatted(member.email().toUpperCase()), team)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.userUuid").value(member.uuid().toString()))
                .andExpect(jsonPath("$.data.role").value("VIEWER"));

        patchAs(owner, "/api/teams/{uuid}/members/{user}", "{\"role\":\"EDITOR\"}", team, member.uuid())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("EDITOR"));

        getAs(member, "/api/teams").andExpect(jsonPath("$.data[0].myRole").value("EDITOR"));
        getAs(member, "/api/teams/{uuid}", team).andExpect(jsonPath("$.data.members.length()").value(2));

        deleteAs(owner, "/api/teams/{uuid}/members/{user}", team, member.uuid()).andExpect(status().isOk());
        getAs(member, "/api/teams/{uuid}", team).andExpect(status().isForbidden());
    }

    @Test
    void addingExistingMemberIs409AndUnknownEmailIs404() throws Exception {
        TestUser owner = registerUser();
        TestUser member = registerUser();
        String team = createTeam(owner, "Dupes");
        addMember(owner, team, member, "VIEWER");
        postAs(owner, "/api/teams/{uuid}/members", """
                {"email":"%s","role":"EDITOR"}""".formatted(member.email()), team)
                .andExpect(status().isConflict());
        postAs(owner, "/api/teams/{uuid}/members", """
                {"email":"ghost@example.com","role":"EDITOR"}""", team)
                .andExpect(status().isNotFound());
    }

    @Test
    void ownerRoleCannotBeAssignedOrRemoved() throws Exception {
        TestUser owner = registerUser();
        TestUser member = registerUser();
        String team = createTeam(owner, "One owner");
        postAs(owner, "/api/teams/{uuid}/members", """
                {"email":"%s","role":"OWNER"}""".formatted(member.email()), team)
                .andExpect(status().isBadRequest());
        patchAs(owner, "/api/teams/{uuid}/members/{user}", "{\"role\":\"VIEWER\"}", team, owner.uuid())
                .andExpect(status().isBadRequest());
        deleteAs(owner, "/api/teams/{uuid}/members/{user}", team, owner.uuid())
                .andExpect(status().isBadRequest());
    }

    @Test
    void nonOwnersCannotManageMembersButCanLeave() throws Exception {
        TestUser owner = registerUser();
        TestUser editor = registerUser();
        TestUser viewer = registerUser();
        TestUser outsider = registerUser();
        String team = createTeam(owner, "Hierarchy");
        addMember(owner, team, editor, "EDITOR");
        addMember(owner, team, viewer, "VIEWER");

        postAs(editor, "/api/teams/{uuid}/members", """
                {"email":"%s","role":"VIEWER"}""".formatted(outsider.email()), team)
                .andExpect(status().isForbidden());
        patchAs(viewer, "/api/teams/{uuid}/members/{user}", "{\"role\":\"EDITOR\"}", team, viewer.uuid())
                .andExpect(status().isForbidden());
        deleteAs(editor, "/api/teams/{uuid}/members/{user}", team, viewer.uuid())
                .andExpect(status().isForbidden());
        deleteAs(viewer, "/api/teams/{uuid}/members/{user}", team, viewer.uuid())
                .andExpect(status().isOk());
    }

    @Test
    void memberWhoDoesNotOwnTeamCannotDeleteIt() throws Exception {
        TestUser owner = registerUser();
        TestUser editor = registerUser();
        String team = createTeam(owner, "Protected");
        addMember(owner, team, editor, "EDITOR");
        deleteAs(editor, "/api/teams/{uuid}", team).andExpect(status().isForbidden());
        deleteAs(owner, "/api/teams/{uuid}", team).andExpect(status().isOk());
        getAs(owner, "/api/teams/{uuid}", team).andExpect(status().isNotFound());
    }

    @Test
    void deletingTeamDetachesButKeepsItsVenues() throws Exception {
        TestUser owner = registerUser();
        String team = createTeam(owner, "Ephemeral");
        String venue = read(postAs(owner, "/api/venues", """
                {"name":"Survivor","teamUuid":"%s"}""".formatted(team)), "$.data.uuid");
        deleteAs(owner, "/api/teams/{uuid}", team).andExpect(status().isOk());
        getAs(owner, "/api/venues/{uuid}", venue)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.teamUuid").isEmpty());
    }

    @Test
    void createValidatesName() throws Exception {
        TestUser owner = registerUser();
        postAs(owner, "/api/teams", "{\"name\":\" \"}").andExpect(status().isBadRequest());
    }
}
