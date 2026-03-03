package com.limitr.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ManualBanRequest(
    @NotBlank String principalId,
    @Min(1) Integer minutes
) {}
