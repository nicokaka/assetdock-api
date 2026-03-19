package com.assetdock.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.seed")
public record LocalSeedProperties(
	boolean enabled,
	String organizationName,
	String organizationSlug,
	String adminFullName,
	String adminEmail,
	String adminPassword
) {
}
