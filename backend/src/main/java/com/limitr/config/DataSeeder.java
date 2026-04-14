package com.limitr.config;

import com.limitr.domain.AdminUser;
import com.limitr.domain.enums.ClientTier;
import com.limitr.repository.AdminUserRepository;
import com.limitr.service.ApiClientService;
import com.limitr.service.RuleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final RuleService ruleService;
    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApiClientService apiClientService;

    @Value("${app.seed.admin.username:admin}")
    private String seedAdminUsername;

    @Value("${app.seed.admin.password:admin12345}")
    private String seedAdminPassword;

    @Value("${app.seed.client.principal-id:local-client}")
    private String seedPrincipalId;

    @Value("${app.seed.client.api-key:local-free-key}")
    private String seedApiKey;

    @Value("${app.seed.enabled:false}")
    private boolean seedEnabled;

    public DataSeeder(
        RuleService ruleService,
        AdminUserRepository adminUserRepository,
        PasswordEncoder passwordEncoder,
        ApiClientService apiClientService
    ) {
        this.ruleService = ruleService;
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.apiClientService = apiClientService;
    }

    @Override
    public void run(String... args) {
        ruleService.getCurrentRule();

        if (!seedEnabled) {
            log.info("Limitr seed data is disabled for this environment.");
            return;
        }

        if (adminUserRepository.findByUsername(seedAdminUsername).isEmpty()) {
            AdminUser user = new AdminUser();
            user.setUsername(seedAdminUsername);
            user.setPasswordHash(passwordEncoder.encode(seedAdminPassword));
            user.setRole("ADMIN");
            adminUserRepository.save(user);
        }

        apiClientService.createIfAbsent(seedPrincipalId, seedApiKey, ClientTier.FREE);

        log.info("Limitr seeded admin username: {}", seedAdminUsername);
        log.info("Limitr local credentials and API key are configured.");
    }
}
