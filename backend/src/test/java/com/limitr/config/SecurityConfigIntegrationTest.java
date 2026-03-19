package com.limitr.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.security.user.password=integration-test-password")
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class SecurityConfigIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void adminEndpointsShouldNotAcceptSpringDefaultBasicAuthUsers() throws Exception {
        String credentials = Base64.getEncoder()
            .encodeToString("user:integration-test-password".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(
            get("/admin/stats")
                .header("Authorization", "Basic " + credentials)
        ).andExpect(status().isUnauthorized());
    }
}
