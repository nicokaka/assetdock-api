package com.assetdock.api.auth.infrastructure;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.web-session")
public record WebSessionProperties(
	Duration idleTimeout,
	Duration absoluteLifetime,
	String sessionCookieName,
	String csrfCookieName,
	boolean secureCookies,
	String sameSite
) {

	public WebSessionProperties {
		if (idleTimeout == null || idleTimeout.isNegative() || idleTimeout.isZero()) {
			throw new IllegalArgumentException("security.web-session.idle-timeout must be greater than 0.");
		}
		if (absoluteLifetime == null || absoluteLifetime.isNegative() || absoluteLifetime.isZero()) {
			throw new IllegalArgumentException("security.web-session.absolute-lifetime must be greater than 0.");
		}
		if (sessionCookieName == null || sessionCookieName.isBlank()) {
			throw new IllegalArgumentException("security.web-session.session-cookie-name is required.");
		}
		if (csrfCookieName == null || csrfCookieName.isBlank()) {
			throw new IllegalArgumentException("security.web-session.csrf-cookie-name is required.");
		}
		if (sameSite == null || sameSite.isBlank()) {
			throw new IllegalArgumentException("security.web-session.same-site is required.");
		}
	}
}
