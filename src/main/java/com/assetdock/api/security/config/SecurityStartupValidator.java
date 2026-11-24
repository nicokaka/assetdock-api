package com.assetdock.api.security.config;

import com.assetdock.api.auth.infrastructure.JwtProperties;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class SecurityStartupValidator implements InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(SecurityStartupValidator.class);

	private static final Set<String> KNOWN_INSECURE_JWT_SECRETS = Set.of(
		"this-is-a-local-dev-secret-only",
		"this-is-a-secure-selfhosted-jwt-secret-key-2026-assetdock",
		"supersecretjwtkeythatisatleast32chars_for_testing_only"
	);
	
	private final Environment environment;
	private final JwtProperties jwtProperties;
	private final String allowedOrigins;

	public SecurityStartupValidator(
		Environment environment,
		JwtProperties jwtProperties,
		@Value("${app.surface.allowed-origins:}") String allowedOrigins
	) {
		this.environment = environment;
		this.jwtProperties = jwtProperties;
		this.allowedOrigins = allowedOrigins;
	}

	@Override
	public void afterPropertiesSet() {
		String[] activeProfiles = environment.getActiveProfiles();
		String[] effectiveProfiles = activeProfiles.length > 0 ? activeProfiles : environment.getDefaultProfiles();
		boolean isLocal = Arrays.asList(effectiveProfiles).contains("local");
		boolean isTest = Arrays.asList(effectiveProfiles).contains("test");

		if (isLocal || isTest) {
			return;
		}

		String secret = jwtProperties.secret();
		if (secret == null || secret.isBlank()) {
			throw new IllegalStateException(
				"CRITICAL SECURITY MISCONFIGURATION: security.jwt.secret is empty in a non-local/test profile. "
					+ "Please set the JWT_SECRET environment variable with a strong secret (at least 32 bytes)."
			);
		}

		if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
			throw new IllegalStateException(
				"CRITICAL SECURITY MISCONFIGURATION: security.jwt.secret must be at least 32 UTF-8 bytes in a non-local/test profile."
			);
		}

		if (KNOWN_INSECURE_JWT_SECRETS.contains(secret)) {
			throw new IllegalStateException(
				"CRITICAL SECURITY MISCONFIGURATION: security.jwt.secret uses a known default/example secret. "
					+ "Generate a unique random JWT_SECRET for production."
			);
		}

		boolean noOriginsConfigured = allowedOrigins == null || allowedOrigins.isBlank()
			|| Arrays.stream(allowedOrigins.split(",")).map(String::trim).allMatch(String::isBlank);
		if (noOriginsConfigured) {
			LOGGER.warn(
				"CORS WARNING: app.surface.allowed-origins is not set in a non-local/test profile. "
					+ "Cross-origin browser requests will be blocked. Set ALLOWED_ORIGINS to your frontend URL."
			);
		}
	}
}
