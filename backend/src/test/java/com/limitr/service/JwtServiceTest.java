package com.limitr.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.limitr.config.JwtProperties;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    @Test
    void generatesAndValidatesTokensWithConfiguredSecret() {
        JwtService jwtService = new JwtService(jwtProperties("secure-secret-key-for-tests-123456", false));

        String token = jwtService.generateToken("admin");

        assertEquals("admin", jwtService.extractUsername(token));
        assertTrue(jwtService.isTokenValid(token, "admin"));
        assertFalse(jwtService.isTokenValid(token, "other-admin"));
    }

    @Test
    void rejectsMissingSecret() {
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> new JwtService(jwtProperties("", false))
        );

        assertEquals("app.jwt.secret must be configured.", exception.getMessage());
    }

    @Test
    void rejectsKnownLocalSecretOutsideAllowedLocalBootstrap() {
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> new JwtService(jwtProperties(JwtProperties.LOCAL_DEVELOPMENT_SECRET, false))
        );

        assertEquals("Configure a unique app.jwt.secret for this environment.", exception.getMessage());
    }

    @Test
    void allowsKnownLocalSecretWhenExplicitlyPermitted() {
        JwtService jwtService = new JwtService(jwtProperties(JwtProperties.LOCAL_DEVELOPMENT_SECRET, true));

        String token = jwtService.generateToken("admin");

        assertTrue(jwtService.isTokenValid(token, "admin"));
    }

    private static JwtProperties jwtProperties(String secret, boolean allowInsecureSecret) {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret(secret);
        jwtProperties.setAllowInsecureSecret(allowInsecureSecret);
        jwtProperties.setExpirationMinutes(120);
        return jwtProperties;
    }
}
