package com.limitr.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
    "spring.security.user.password=integration-test-password",
    "app.auth.registration-mode=disabled"
})
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class DisabledRegistrationModeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void registerEndpointReturnsDisabledMessageWhenRegistrationModeIsDisabled() throws Exception {
        mockMvc.perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "new-admin",
                      "password": "password123"
                    }
                    """)
        ).andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("Admin registration is disabled."));
    }
}
