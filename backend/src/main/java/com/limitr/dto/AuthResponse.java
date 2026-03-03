package com.limitr.dto;

public record AuthResponse(
    String accessToken,
    String tokenType
) {}
