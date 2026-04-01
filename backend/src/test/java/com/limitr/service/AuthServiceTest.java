package com.limitr.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.limitr.domain.AdminUser;
import com.limitr.repository.AdminUserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest {

    private final AdminUserRepository adminUserRepository = org.mockito.Mockito.mock(AdminUserRepository.class);
    private final PasswordEncoder passwordEncoder = org.mockito.Mockito.mock(PasswordEncoder.class);
    private final TestJwtService jwtService = new TestJwtService();
    private final RecordingAbuseDetectionService abuseDetectionService = new RecordingAbuseDetectionService();

    private final AuthService authService = new AuthService(
        adminUserRepository,
        passwordEncoder,
        jwtService,
        abuseDetectionService
    );

    @Test
    void recordsFailedAttemptForUnknownUsername() {
        when(adminUserRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> authService.login("ghost", "wrong-password")
        );

        assertEquals("Invalid credentials.", exception.getMessage());
        assertEquals(List.of("admin:ghost"), abuseDetectionService.failedAttempts);
    }

    @Test
    void recordsFailedAttemptForWrongPassword() {
        AdminUser user = new AdminUser();
        user.setUsername("admin");
        user.setPasswordHash("stored-hash");

        when(adminUserRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "stored-hash")).thenReturn(false);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> authService.login("admin", "wrong-password")
        );

        assertEquals("Invalid credentials.", exception.getMessage());
        assertEquals(List.of("admin:admin"), abuseDetectionService.failedAttempts);
    }

    @Test
    void returnsTokenWithoutRecordingFailedAttemptForValidCredentials() {
        AdminUser user = new AdminUser();
        user.setUsername("admin");
        user.setPasswordHash("stored-hash");

        when(adminUserRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct-password", "stored-hash")).thenReturn(true);

        String token = authService.login("admin", "correct-password");

        assertEquals("jwt-token", token);
        assertEquals(List.of(), abuseDetectionService.failedAttempts);
    }

    @Test
    void registerCreatesFirstAdminDuringBootstrap() {
        when(adminUserRepository.count()).thenReturn(0L);
        when(adminUserRepository.findByUsername("first-admin")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");

        authService.register("first-admin", "password123");

        verify(adminUserRepository).save(any(AdminUser.class));
    }

    @Test
    void registerIsClosedOnceAnAdminAlreadyExists() {
        when(adminUserRepository.count()).thenReturn(1L);

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> authService.register("second-admin", "password123")
        );

        assertEquals("Admin registration is closed.", exception.getMessage());
        verify(adminUserRepository, never()).findByUsername(any());
        verify(adminUserRepository, never()).save(any(AdminUser.class));
        verify(passwordEncoder, never()).encode(any());
    }

    private static class TestJwtService extends JwtService {

        @Override
        public String generateToken(String username) {
            return "jwt-token";
        }
    }

    private static class RecordingAbuseDetectionService extends AbuseDetectionService {

        private final List<String> failedAttempts = new ArrayList<>();

        @Override
        public void recordFailedAuthAttempt(String principalId) {
            failedAttempts.add(principalId);
        }
    }
}
