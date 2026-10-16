package io.trysona.sona.user;

import io.trysona.sona.auth.RefreshTokenRepository;
import io.trysona.sona.common.Jsonb;
import io.trysona.sona.exception.BadRequestException;
import io.trysona.sona.user.dto.ChangePasswordRequest;
import io.trysona.sona.user.dto.UpdateUserRequest;
import io.trysona.sona.user.dto.UserResponse;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserResponse getMe(Long userId) {
        return UserResponse.from(load(userId));
    }

    @Transactional
    public UserResponse update(Long userId, UpdateUserRequest request) {
        User user = load(userId);
        if (request != null) {
            if (request.role() != null) {
                user.setRole(request.role());
            }
            if (request.preferences() != null) {
                user.setPreferences(Jsonb.merge(user.getPreferences(), request.preferences()));
            }
        }
        return UserResponse.from(userRepository.saveAndFlush(user));
    }

    /**
     * Changing the password signs the user out everywhere else by revoking all refresh tokens.
     */
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = load(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        refreshTokenRepository.revokeAllForUser(userId);
        log.info("Password changed for user {}", user.getUuid());
    }

    /**
     * Deletes the account; owned venues, shows, presets, teams and share links cascade in the database.
     */
    @Transactional
    public void delete(Long userId) {
        User user = load(userId);
        userRepository.delete(user);
        log.info("Deleted user {}", user.getUuid());
    }

    User load(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }
}
