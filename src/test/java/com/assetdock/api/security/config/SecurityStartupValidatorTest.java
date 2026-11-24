package com.assetdock.api.security.config;

import com.assetdock.api.auth.infrastructure.JwtProperties;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityStartupValidatorTest {

	@Test
	void rejectsKnownDefaultJwtSecretInProductionEvenWhenDefaultProfilesIncludeLocal() {
		MockEnvironment environment = new MockEnvironment();
		environment.setDefaultProfiles("local");
		environment.setActiveProfiles("production");

		SecurityStartupValidator validator = new SecurityStartupValidator(
			environment,
			new JwtProperties("this-is-a-secure-selfhosted-jwt-secret-key-2026-assetdock", "assetdock-api", Duration.ofMinutes(15)),
			"https://assetdock.example.com"
		);

		assertThatThrownBy(validator::afterPropertiesSet)
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("known default/example secret");
	}

	@Test
	void rejectsShortUtf8SecretInProduction() {
		MockEnvironment environment = new MockEnvironment();
		environment.setActiveProfiles("production");

		SecurityStartupValidator validator = new SecurityStartupValidator(
			environment,
			new JwtProperties("short-secret", "assetdock-api", Duration.ofMinutes(15)),
			"https://assetdock.example.com"
		);

		assertThatThrownBy(validator::afterPropertiesSet)
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("at least 32 UTF-8 bytes");
	}

	@Test
	void allowsStrongUniqueSecretInProduction() {
		MockEnvironment environment = new MockEnvironment();
		environment.setActiveProfiles("production");

		SecurityStartupValidator validator = new SecurityStartupValidator(
			environment,
			new JwtProperties("a-very-strong-randomly-generated-production-key-0123456789", "assetdock-api", Duration.ofMinutes(15)),
			"https://assetdock.example.com"
		);

		assertThatCode(validator::afterPropertiesSet).doesNotThrowAnyException();
	}

	@Test
	void skipsValidationWhenRunningInLocalProfile() {
		MockEnvironment environment = new MockEnvironment();
		environment.setDefaultProfiles("local");

		SecurityStartupValidator validator = new SecurityStartupValidator(
			environment,
			new JwtProperties("this-is-a-local-dev-secret-only", "assetdock-api", Duration.ofMinutes(15)),
			""
		);

		assertThatCode(validator::afterPropertiesSet).doesNotThrowAnyException();
	}
}
