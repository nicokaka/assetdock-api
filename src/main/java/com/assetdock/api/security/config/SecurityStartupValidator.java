package com.assetdock.api.security.config;

import com.assetdock.api.auth.infrastructure.JwtProperties;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class SecurityStartupValidator implements InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(SecurityStartupValidator.class);
	
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
		boolean isLocal = Arrays.asList(environment.getActiveProfiles()).contains("local")
			|| Arrays.asList(environment.getDefaultProfiles()).contains("local");
		boolean isTest = Arrays.asList(environment.getActiveProfiles()).contains("test")
			|| Arrays.asList(environment.getDefaultProfiles()).contains("test");

		if (isLocal || isTest) {
			return;
		}

		if (jwtProperties.secret() == null || jwtProperties.secret().isBlank()) {
			throw new IllegalStateException(
				"CRITICAL SECURITY MISCONFIGURATION: security.jwt.secret is empty in a non-local/test profile. "
					+ "Please set the JWT_SECRET environment variable with a strong secret (at least 32 bytes)."
			);
		}
		
		if (jwtProperties.secret().length() < 32) {
			LOGGER.warn("SECURITY WARNING: security.jwt.secret is less than 32 characters long. This is insecure.");
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
