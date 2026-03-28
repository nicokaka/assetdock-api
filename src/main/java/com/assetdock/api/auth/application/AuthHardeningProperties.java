package com.assetdock.api.auth.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.auth")
public record AuthHardeningProperties(int maxFailedLoginAttempts) {

	public AuthHardeningProperties {
		if (maxFailedLoginAttempts < 1) {
			throw new IllegalArgumentException("security.auth.max-failed-login-attempts must be at least 1.");
		}
	}
}
