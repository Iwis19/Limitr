package com.shieldgate.dto;

public record AuthResponse(
    String accessToken,
    String tokenType
) {}
