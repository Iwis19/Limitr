package com.limitr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminUserCreateRequest(
    @NotBlank String username,
    @NotBlank @Size(min = 8, max = 72) String password
) {}
