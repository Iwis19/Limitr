package com.limitr.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ApiProtectionFilterTest {

    private final ApiProtectionFilter filter = new ApiProtectionFilter(null, null, null, null, null, null, null);

    @Test
    void skipsPublicApiRoutes() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/public/ping");
        request.setServletPath("/api/public/ping");

        assertTrue(filter.shouldNotFilter(request));
    }

    @Test
    void stillProtectsNonPublicApiRoutes() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/data");
        request.setServletPath("/api/data");

        assertFalse(filter.shouldNotFilter(request));
    }
}
