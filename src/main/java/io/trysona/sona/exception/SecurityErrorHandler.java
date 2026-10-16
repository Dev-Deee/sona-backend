package io.trysona.sona.exception;

import io.trysona.sona.auth.jwt.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Writes the standard error envelope for failures that happen inside the security filter chain,
 * before a controller (and therefore {@link GlobalExceptionHandler}) is reached.
 */
@Component
public class SecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final JsonMapper jsonMapper;

    public SecurityErrorHandler(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        Object tokenError = request.getAttribute(JwtAuthenticationFilter.TOKEN_ERROR_ATTRIBUTE);
        ApiError body;
        if (tokenError instanceof ExpiredTokenException ex) {
            body = ApiError.of("TOKEN_EXPIRED", ex.getMessage());
        } else if (tokenError instanceof InvalidTokenException ex) {
            body = ApiError.of("INVALID_TOKEN", ex.getMessage());
        } else {
            body = ApiError.of("UNAUTHORIZED", "Authentication is required");
        }
        write(response, HttpStatus.UNAUTHORIZED, body);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        write(response, HttpStatus.FORBIDDEN, ApiError.of("FORBIDDEN", "You do not have access to this resource"));
    }

    public void write(HttpServletResponse response, HttpStatus status, ApiError body) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        jsonMapper.writeValue(response.getOutputStream(), body);
    }
}
