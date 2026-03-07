package com.limitr.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RulesUpdateRequestValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDownValidator() {
        validatorFactory.close();
    }

    @Test
    void rejectsThrottledLimitAboveBaseLimit() {
        RulesUpdateRequest request = new RulesUpdateRequest(10, 11, 2, 4, 7, 15);

        Set<String> messages = validator.validate(request)
            .stream()
            .map(violation -> violation.getMessage())
            .collect(Collectors.toSet());

        assertTrue(messages.contains("Throttled limit per minute cannot exceed the base limit."));
    }

    @Test
    void rejectsThresholdsThatDoNotIncrease() {
        RulesUpdateRequest request = new RulesUpdateRequest(60, 20, 5, 2, 1, 15);

        Set<String> messages = validator.validate(request)
            .stream()
            .map(violation -> violation.getMessage())
            .collect(Collectors.toSet());

        assertTrue(messages.contains("Thresholds must increase from warn to throttle to ban."));
    }

    @Test
    void acceptsValidRuleConfiguration() {
        RulesUpdateRequest request = new RulesUpdateRequest(60, 20, 2, 4, 7, 15);

        Set<String> messages = validator.validate(request)
            .stream()
            .map(violation -> violation.getMessage())
            .collect(Collectors.toSet());

        assertEquals(Set.of(), messages);
    }
}
