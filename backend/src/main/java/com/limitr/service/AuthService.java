package com.limitr.service;

import com.limitr.domain.AdminUser;
import com.limitr.repository.AdminUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AbuseDetectionService abuseDetectionService;

    public AuthService(
        AdminUserRepository adminUserRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService,
        AbuseDetectionService abuseDetectionService
    ) {
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.abuseDetectionService = abuseDetectionService;
    }

    @Transactional
    public void register(String username, String password) {
        if (adminUserRepository.count() > 0) {
            throw new IllegalStateException("Admin registration is closed.");
        }

        if (adminUserRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username is already registered.");
        }

        AdminUser adminUser = new AdminUser();
        adminUser.setUsername(username);
        adminUser.setPasswordHash(passwordEncoder.encode(password));
        adminUser.setRole("ADMIN");
        adminUserRepository.save(adminUser);
    }

    public String login(String username, String password) {
        AdminUser user = adminUserRepository.findByUsername(username).orElse(null);
        if (user == null) {
            throw invalidCredentials(username);
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw invalidCredentials(username);
        }

        return jwtService.generateToken(user.getUsername());
    }

    private IllegalArgumentException invalidCredentials(String username) {
        abuseDetectionService.recordFailedAuthAttempt("admin:" + username);
        return new IllegalArgumentException("Invalid credentials.");
    }
}
