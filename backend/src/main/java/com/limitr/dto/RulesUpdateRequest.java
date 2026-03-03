package com.limitr.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RulesUpdateRequest(
    @NotNull @Min(1) Integer baseLimitPerMinute,
    @NotNull @Min(1) Integer throttledLimitPerMinute,
    @NotNull @Min(0) Integer warnThreshold,
    @NotNull @Min(1) Integer throttleThreshold,
    @NotNull @Min(1) Integer banThreshold,
    @NotNull @Min(1) Integer banMinutes
) {}
