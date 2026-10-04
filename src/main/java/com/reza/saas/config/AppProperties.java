package com.reza.saas.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Typed view of the "app.*" block in application.yml. */
@ConfigurationProperties(prefix = "app")
@Validated
public record AppProperties(Dynamodb dynamodb, Billing billing) {

    public record Dynamodb(@NotBlank String tableName) {
    }

    public record Billing(@NotBlank String provider) {
    }
}