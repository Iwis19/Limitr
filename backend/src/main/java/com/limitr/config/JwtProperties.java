package com.limitr.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    public static final String LOCAL_DEVELOPMENT_SECRET =
        "limitr-local-secret-key-change-this-in-production-1234567890";

    private String secret = "";
    private long expirationMinutes = 120;
    private boolean allowInsecureSecret;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret == null ? "" : secret;
    }

    public long getExpirationMinutes() {
        return expirationMinutes;
    }

    public void setExpirationMinutes(long expirationMinutes) {
        this.expirationMinutes = expirationMinutes;
    }

    public boolean isAllowInsecureSecret() {
        return allowInsecureSecret;
    }

    public void setAllowInsecureSecret(boolean allowInsecureSecret) {
        this.allowInsecureSecret = allowInsecureSecret;
    }
}
