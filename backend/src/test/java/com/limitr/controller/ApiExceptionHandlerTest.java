package com.limitr.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ApiExceptionHandlerTest {

    private final MockMvc mockMvc = MockMvcBuilders
        .standaloneSetup(
            new AdminController(null, null, null, null, null),
            new SampleApiController()
        )
        .setControllerAdvice(new ApiExceptionHandler())
        .build();

    @Test
    void returnsValidationMessageForInvalidRuleConfiguration() throws Exception {
        mockMvc.perform(put("/admin/rules")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "baseLimitPerMinute": 1,
                      "throttledLimitPerMinute": 99,
                      "warnThreshold": 2,
                      "throttleThreshold": 4,
                      "banThreshold": 7,
                      "banMinutes": 15
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Throttled limit per minute cannot exceed the base limit."));
    }

    @Test
    void returnsConsistentErrorForMalformedJson() throws Exception {
        mockMvc.perform(put("/admin/rules")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"baseLimitPerMinute\":"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Malformed JSON request body."));
    }

    @Test
    void returnsConsistentErrorForPathVariableTypeMismatch() throws Exception {
        mockMvc.perform(get("/api/resource/not-a-number"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Invalid value for 'id'."));
    }
}
