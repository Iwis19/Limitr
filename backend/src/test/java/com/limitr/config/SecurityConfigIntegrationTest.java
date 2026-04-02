package com.limitr.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = "spring.security.user.password=integration-test-password")
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class SecurityConfigIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void adminEndpointsShouldNotAcceptSpringDefaultBasicAuthUsers() throws Exception {
        String credentials = Base64.getEncoder()
            .encodeToString("user:integration-test-password".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(
            get("/admin/stats")
                .header("Authorization", "Basic " + credentials)
        ).andExpect(status().isUnauthorized());
    }

    @Test
    void seededAdminCanLoginAndAccessAdminEndpointsWithJwt() throws Exception {
        MvcResult loginResult = mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "admin",
                      "password": "admin12345"
                    }
                    """)
        ).andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isString())
            .andReturn();

        JsonNode loginPayload = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String accessToken = loginPayload.get("accessToken").asText();

        mockMvc.perform(
            get("/admin/stats")
                .header("Authorization", "Bearer " + accessToken)
        ).andExpect(status().isOk());
    }

    @Test
    void registerEndpointIsClosedAfterBootstrapAdminExists() throws Exception {
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
            .andExpect(jsonPath("$.error").value("Admin registration is closed."));
    }

    @Test
    void seededAdminCanCreateAnotherAdminThroughAdminEndpoint() throws Exception {
        String accessToken = login("admin", "admin12345");

        mockMvc.perform(
            post("/admin/users")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "ops-admin",
                      "password": "password123"
                    }
                    """)
        ).andExpect(status().isCreated())
            .andExpect(jsonPath("$.username").value("ops-admin"));

        mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "ops-admin",
                      "password": "password123"
                    }
                    """)
        ).andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isString());
    }

    private String login(String username, String password) throws Exception {
        MvcResult loginResult = mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "%s",
                      "password": "%s"
                    }
                    """.formatted(username, password))
        ).andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isString())
            .andReturn();

        JsonNode loginPayload = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        return loginPayload.get("accessToken").asText();
    }
}
