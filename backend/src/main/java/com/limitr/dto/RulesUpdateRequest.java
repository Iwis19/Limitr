package com.limitr.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RulesUpdateRequest(
    @NotNull @Min(1) Integer baseLimitPerMinute,
    @NotNull @Min(1) Integer throttledLimitPerMinute,
    @NotNull @Min(0) Integer warnThreshold,
    @NotNull @Min(1) Integer throttleThreshold,
    @NotNull @Min(1) Integer banThreshold,
    @NotNull @Min(1) Integer banMinutes
) {
    @AssertTrue(message = "Throttled limit per minute cannot exceed the base limit.")
    public boolean hasValidRateLimits() {
        if (baseLimitPerMinute == null || throttledLimitPerMinute == null) {
            return true;
        }
        return throttledLimitPerMinute <= baseLimitPerMinute;
    }

    @AssertTrue(message = "Thresholds must increase from warn to throttle to ban.")
    public boolean hasAscendingThresholds() {
        if (warnThreshold == null || throttleThreshold == null || banThreshold == null) {
            return true;
        }
        return warnThreshold < throttleThreshold && throttleThreshold < banThreshold;
    }
}
