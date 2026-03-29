package com.assetdock.api.auth.api;

import com.assetdock.api.auth.infrastructure.JwtTokenService;
import com.assetdock.api.security.auth.AuthenticatedUserPrincipal;
import com.assetdock.api.user.domain.UserRole;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class AuthTokenBoundaryIntegrationTest {

	private static final UUID USER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
	private static final UUID ORGANIZATION_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

	@Container
	static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
		.withDatabaseName("assetdock_test")
		.withUsername("assetdock")
		.withPassword("assetdock");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtTokenService jwtTokenService;

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRES::getUsername);
		registry.add("spring.datasource.password", POSTGRES::getPassword);
		registry.add("security.jwt.secret", () -> "test-only-jwt-secret-key-with-32-bytes");
	}

	@Test
	void malformedJwtIsRejected() throws Exception {
		mockMvc.perform(get("/assets")
				.header(AUTHORIZATION, "Bearer not-a-jwt"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void invalidSignatureJwtIsRejected() throws Exception {
		String validToken = validToken();
		String invalidSignatureToken = validToken.substring(0, validToken.length() - 1)
			+ (validToken.endsWith("a") ? "b" : "a");

		mockMvc.perform(get("/assets")
				.header(AUTHORIZATION, "Bearer " + invalidSignatureToken))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void expiredJwtIsRejected() throws Exception {
		String expiredToken = jwtTokenService.issue(principal(), Instant.now().minusSeconds(3600)).value();

		mockMvc.perform(get("/assets")
				.header(AUTHORIZATION, "Bearer " + expiredToken))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void authorizationHeaderWithoutBearerSchemeIsRejected() throws Exception {
		mockMvc.perform(get("/assets")
				.header(AUTHORIZATION, validToken()))
			.andExpect(status().isUnauthorized());
	}

	private String validToken() {
		return jwtTokenService.issue(principal(), Instant.now()).value();
	}

	private AuthenticatedUserPrincipal principal() {
		return new AuthenticatedUserPrincipal(
			USER_ID,
			ORGANIZATION_ID,
			"user@assetdock.dev",
			Set.of(UserRole.ORG_ADMIN)
		);
	}
}
