package com.limitr.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.limitr.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class JwtStartupValidationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
        .withUserConfiguration(JwtServiceConfiguration.class);

    @Test
    void startupFailsWhenJwtSecretIsMissing() {
        contextRunner.run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                .hasRootCauseInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("app.jwt.secret must be configured.");
        });
    }

    @Test
    void startupFailsWhenUsingTheKnownLocalSecretWithoutExplicitOptIn() {
        contextRunner.withPropertyValues(
            "app.jwt.secret=" + JwtProperties.LOCAL_DEVELOPMENT_SECRET
        ).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                .hasRootCauseInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("Configure a unique app.jwt.secret for this environment.");
        });
    }

    @Test
    void startupSucceedsWithSecureJwtSecret() {
        contextRunner.withPropertyValues(
            "app.jwt.secret=secure-context-secret-for-tests-123456"
        ).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(JwtService.class);
        });
    }

    @Test
    void startupAllowsLocalSecretWhenExplicitlyEnabledForLocalBootstrap() {
        contextRunner.withPropertyValues(
            "app.jwt.secret=" + JwtProperties.LOCAL_DEVELOPMENT_SECRET,
            "app.jwt.allow-insecure-secret=true"
        ).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(JwtService.class);
        });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(JwtProperties.class)
    static class JwtServiceConfiguration {

        @Bean
        JwtService jwtService(JwtProperties jwtProperties) {
            return new JwtService(jwtProperties);
        }
    }
}
