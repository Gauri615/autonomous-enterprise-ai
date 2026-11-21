package com.autonomousenterprise.api.metadata;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "enterprise.api")
public record ApiMetadataProperties(String version) {
}
