package com.limitr.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.limitr.domain.ApiClient;
import com.limitr.domain.RuleConfig;
import com.limitr.domain.enums.ClientTier;
import com.limitr.repository.AdminUserRepository;
import com.limitr.service.ApiClientService;
import com.limitr.service.RuleService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class DataSeederTest {

    private final TestRuleService ruleService = new TestRuleService();
    private final AdminUserRepository adminUserRepository = org.mockito.Mockito.mock(AdminUserRepository.class);
    private final PasswordEncoder passwordEncoder = org.mockito.Mockito.mock(PasswordEncoder.class);
    private final RecordingApiClientService apiClientService = new RecordingApiClientService();

    private final DataSeeder dataSeeder = new DataSeeder(
        ruleService,
        adminUserRepository,
        passwordEncoder,
        apiClientService
    );

    @Test
    void runSkipsCredentialSeedingWhenDisabled() {
        ReflectionTestUtils.setField(dataSeeder, "seedEnabled", false);

        dataSeeder.run();

        assertTrue(ruleService.called);
        verify(adminUserRepository, never()).findByUsername(any());
        verify(adminUserRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
        assertFalse(apiClientService.called);
    }

    @Test
    void runSeedsConfiguredCredentialsWhenEnabled() {
        ReflectionTestUtils.setField(dataSeeder, "seedEnabled", true);
        ReflectionTestUtils.setField(dataSeeder, "seedAdminUsername", "local-admin");
        ReflectionTestUtils.setField(dataSeeder, "seedAdminPassword", "local-password");
        ReflectionTestUtils.setField(dataSeeder, "seedPrincipalId", "local-principal");
        ReflectionTestUtils.setField(dataSeeder, "seedApiKey", "local-api-key");
        when(adminUserRepository.findByUsername("local-admin")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("local-password")).thenReturn("encoded-password");

        dataSeeder.run();

        assertTrue(ruleService.called);
        verify(adminUserRepository).findByUsername("local-admin");
        verify(adminUserRepository).save(any());
        verify(passwordEncoder).encode("local-password");
        assertTrue(apiClientService.called);
        assertEquals("local-principal", apiClientService.principalId);
        assertEquals("local-api-key", apiClientService.apiKey);
        assertEquals(ClientTier.FREE, apiClientService.tier);
    }

    private static class TestRuleService extends RuleService {

        private boolean called;

        TestRuleService() {
            super(null);
        }

        @Override
        public RuleConfig getCurrentRule() {
            called = true;
            return RuleConfig.defaults();
        }
    }

    private static class RecordingApiClientService extends ApiClientService {

        private boolean called;
        private String principalId;
        private String apiKey;
        private ClientTier tier;

        RecordingApiClientService() {
            super(null, null);
        }

        @Override
        public ApiClient createIfAbsent(String principalId, String apiKey, ClientTier tier) {
            this.called = true;
            this.principalId = principalId;
            this.apiKey = apiKey;
            this.tier = tier;
            return new ApiClient();
        }
    }
}
