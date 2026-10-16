package io.trysona.sona.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import io.trysona.sona.TestcontainersConfiguration;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.json.JsonMapper;

/**
 * Base class for full-stack tests: real Spring context, real PostgreSQL (Testcontainers), MockMvc.
 * The context (and container) is shared by every subclass. Each test registers fresh users with
 * random emails so tests never depend on one another's data.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
public abstract class IntegrationTest {

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected JsonMapper json;

    protected record TestUser(String email, String password, String accessToken, String refreshToken, UUID uuid) {
    }

    protected TestUser registerUser() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        String password = "correct-horse-battery";
        MvcResult result = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s","role":"TOURING"}""".formatted(email, password)))
                .andExpect(status().isCreated())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        return new TestUser(email, password,
                JsonPath.read(body, "$.data.accessToken"),
                JsonPath.read(body, "$.data.refreshToken"),
                UUID.fromString(JsonPath.read(body, "$.data.user.uuid")));
    }

    protected ResultActions getAs(TestUser user, String url, Object... vars) throws Exception {
        return mvc.perform(auth(get(url, vars), user));
    }

    protected ResultActions postAs(TestUser user, String url, String body, Object... vars) throws Exception {
        return mvc.perform(auth(post(url, vars), user).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    protected ResultActions patchAs(TestUser user, String url, String body, Object... vars) throws Exception {
        MockHttpServletRequestBuilder request = auth(patch(url, vars), user);
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return mvc.perform(request);
    }

    protected ResultActions deleteAs(TestUser user, String url, Object... vars) throws Exception {
        return mvc.perform(auth(delete(url, vars), user));
    }

    protected String createVenue(TestUser user, String name) throws Exception {
        return read(postAs(user, "/api/venues", """
                {"name":"%s","dimensions":{"length":30,"width":20,"height":10,"unit":"m"}}""".formatted(name))
                .andExpect(status().isCreated()), "$.data.uuid");
    }

    protected String createTeam(TestUser owner, String name) throws Exception {
        return read(postAs(owner, "/api/teams", """
                {"name":"%s"}""".formatted(name)).andExpect(status().isCreated()), "$.data.uuid");
    }

    protected void addMember(TestUser owner, String teamUuid, TestUser member, String role) throws Exception {
        postAs(owner, "/api/teams/{uuid}/members", """
                {"email":"%s","role":"%s"}""".formatted(member.email(), role), teamUuid)
                .andExpect(status().isCreated());
    }

    protected static <T> T read(ResultActions actions, String path) throws Exception {
        return JsonPath.read(actions.andReturn().getResponse().getContentAsString(), path);
    }

    private static MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder request, TestUser user) {
        return user == null ? request : request.header(HttpHeaders.AUTHORIZATION, "Bearer " + user.accessToken());
    }
}
