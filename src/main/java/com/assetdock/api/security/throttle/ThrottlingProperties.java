package com.assetdock.api.security.throttle;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.throttle")
public record ThrottlingProperties(
	EndpointPolicy login,
	EndpointPolicy assetImport
) {

	public ThrottlingProperties {
		if (login == null) {
			throw new IllegalArgumentException("security.throttle.login must be configured.");
		}
		if (assetImport == null) {
			throw new IllegalArgumentException("security.throttle.asset-import must be configured.");
		}
	}

	public record EndpointPolicy(
		boolean enabled,
		int maxRequests,
		Duration window,
		int maxTrackedClients
	) {

		public EndpointPolicy {
			if (maxRequests < 1) {
				throw new IllegalArgumentException("maxRequests must be at least 1.");
			}
			if (window == null || window.isZero() || window.isNegative()) {
				throw new IllegalArgumentException("window must be a positive duration.");
			}
			if (maxTrackedClients < 1) {
				throw new IllegalArgumentException("maxTrackedClients must be at least 1.");
			}
		}
	}
}
