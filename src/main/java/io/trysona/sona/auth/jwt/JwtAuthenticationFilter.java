package io.trysona.sona.auth.jwt;

import io.trysona.sona.auth.AuthUser;
import io.trysona.sona.exception.ExpiredTokenException;
import io.trysona.sona.exception.InvalidTokenException;
import io.trysona.sona.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates requests carrying {@code Authorization: Bearer <jwt>}. An invalid token does not
 * short-circuit the request: public endpoints still work, and protected ones are rejected by the
 * entry point, which reads the reason from {@link #TOKEN_ERROR_ATTRIBUTE}.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String TOKEN_ERROR_ATTRIBUTE = "sona.tokenError";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length()).trim();
            try {
                UUID userUuid = jwtService.validateAccessToken(token);
                // Looking the user up on every request means tokens for deleted accounts stop working immediately.
                userRepository.findByUuid(userUuid).ifPresentOrElse(user -> {
                    AuthUser principal = new AuthUser(user.getId(), user.getUuid(), user.getEmail());
                    var authentication = new UsernamePasswordAuthenticationToken(
                            principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }, () -> request.setAttribute(TOKEN_ERROR_ATTRIBUTE,
                        new InvalidTokenException("Account no longer exists")));
            } catch (ExpiredTokenException | InvalidTokenException e) {
                request.setAttribute(TOKEN_ERROR_ATTRIBUTE, e);
            }
        }
        chain.doFilter(request, response);
    }
}
