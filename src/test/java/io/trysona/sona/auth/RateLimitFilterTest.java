package io.trysona.sona.auth;

import static org.assertj.core.api.Assertions.assertThat;

import io.trysona.sona.exception.SecurityErrorHandler;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

class RateLimitFilterTest {

    private final RateLimitFilter filter = new RateLimitFilter(10, new SecurityErrorHandler(JsonMapper.builder().build()));

    @Test
    void allowsTenAuthRequestsPerSecondPerIpThenReturns429() throws Exception {
        for (int i = 0; i < 10; i++) {
            assertThat(call("/api/auth/login", "10.0.0.1").getStatus()).isEqualTo(200);
        }
        MockHttpServletResponse limited = call("/api/auth/login", "10.0.0.1");
        assertThat(limited.getStatus()).isEqualTo(429);
        assertThat(limited.getHeader("Retry-After")).isEqualTo("1");
        assertThat(limited.getContentAsString()).contains("RATE_LIMITED");

        // A different client is unaffected.
        assertThat(call("/api/auth/login", "10.0.0.2").getStatus()).isEqualTo(200);
    }

    @Test
    void doesNotLimitNonAuthEndpoints() throws Exception {
        for (int i = 0; i < 50; i++) {
            assertThat(call("/api/venues", "10.0.0.3").getStatus()).isEqualTo(200);
        }
    }

    private MockHttpServletResponse call(String uri, String ip) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", uri);
        request.setRemoteAddr(ip);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
