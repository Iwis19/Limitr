package com.limitr.config;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.limitr.repository.ApiClientRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
    "spring.security.user.password=integration-test-password",
    "app.seed.enabled=false",
    "spring.datasource.url=jdbc:h2:mem:limitr-seeding-disabled;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class SeedingDisabledIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApiClientRepository apiClientRepository;

    @Test
    void loginFailsWhenSeededAdminIsDisabled() throws Exception {
        mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "admin",
                      "password": "admin12345"
                    }
                    """)
        ).andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("Invalid credentials."));
    }

    @Test
    void seededApiClientIsNotCreatedWhenSeedingIsDisabled() {
        assertTrue(apiClientRepository.findByPrincipalId("local-client").isEmpty());
    }
}
