package io.trysona.sona.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.trysona.sona.auth.RefreshTokenRepository;
import io.trysona.sona.exception.BadRequestException;
import io.trysona.sona.support.Fixtures;
import io.trysona.sona.user.dto.ChangePasswordRequest;
import io.trysona.sona.user.dto.UpdateUserRequest;
import jakarta.persistence.EntityNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks UserService service;

    @Test
    void getMeThrowsWhenMissing() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getMe(1L)).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void updateWithNullRequestIsNoOp() {
        User user = Fixtures.user(1);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.saveAndFlush(user)).thenReturn(user);
        assertThat(service.update(1L, null).role()).isEqualTo(UserRole.TOURING);
    }

    @Test
    void updateMergesPreferences() {
        User user = Fixtures.user(1);
        user.setPreferences(new HashMap<>(Map.of("units", "metric", "theme", "dark")));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.saveAndFlush(user)).thenReturn(user);
        Map<String, Object> patch = new HashMap<>();
        patch.put("units", "imperial");
        patch.put("theme", null);

        var response = service.update(1L, new UpdateUserRequest(UserRole.STUDENT, patch));

        assertThat(response.role()).isEqualTo(UserRole.STUDENT);
        assertThat(response.preferences()).containsExactlyEntriesOf(Map.of("units", "imperial"));
    }

    @Test
    void changePasswordRejectsWrongCurrent() {
        User user = Fixtures.user(1);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("bad", user.getPassword())).thenReturn(false);
        assertThatThrownBy(() -> service.changePassword(1L, new ChangePasswordRequest("bad", "new-password")))
                .isInstanceOf(BadRequestException.class);
        verify(refreshTokenRepository, never()).revokeAllForUser(any());
    }

    @Test
    void changePasswordHashesAndRevokesSessions() {
        User user = Fixtures.user(1);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old", "$2a$hash")).thenReturn(true);
        when(passwordEncoder.encode("new-password")).thenReturn("$2a$new");
        service.changePassword(1L, new ChangePasswordRequest("old", "new-password"));
        assertThat(user.getPassword()).isEqualTo("$2a$new");
        verify(refreshTokenRepository).revokeAllForUser(1L);
    }

    @Test
    void deleteRemovesUser() {
        User user = Fixtures.user(1);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        service.delete(1L);
        verify(userRepository).delete(user);
    }

    @Test
    void toStringNeverContainsPasswordOrEmail() {
        User user = Fixtures.user(1);
        assertThat(user.toString()).doesNotContain("$2a$hash").doesNotContain("@");
        assertThat(new ChangePasswordRequest("a-secret", "b-secret").toString()).doesNotContain("secret");
    }
}
