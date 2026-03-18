package com.assetdock.api.auth.api;

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class AuthLoginIntegrationTest {

	private static final UUID ORGANIZATION_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
	private static final UUID USER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

	@Container
	static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
		.withDatabaseName("assetdock_test")
		.withUsername("assetdock")
		.withPassword("assetdock");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRES::getUsername);
		registry.add("spring.datasource.password", POSTGRES::getPassword);
		registry.add("security.jwt.secret", () -> "test-only-jwt-secret-key-with-32-bytes");
	}

	@BeforeEach
	void setUp() {
		cleanDatabase();

		jdbcTemplate.update(
			"INSERT INTO organizations (id, name, slug) VALUES (?, ?, ?)",
			ORGANIZATION_ID,
			"AssetDock",
			"assetdock"
		);
	}

	@AfterEach
	void tearDown() {
		cleanDatabase();
	}

	@Test
	void shouldLoginSuccessfully() throws Exception {
		insertUser("ACTIVE", "user@assetdock.dev", "S3curePass!");

		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "email": "user@assetdock.dev",
					  "password": "S3curePass!"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accessToken").isString())
			.andExpect(jsonPath("$.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.user.userId").value(USER_ID.toString()))
			.andExpect(jsonPath("$.user.organizationId").value(ORGANIZATION_ID.toString()))
			.andExpect(jsonPath("$.user.roles[0]").value("ORG_ADMIN"));
	}

	@Test
	void shouldRejectInvalidCredentials() throws Exception {
		insertUser("ACTIVE", "user@assetdock.dev", "S3curePass!");

		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "email": "user@assetdock.dev",
					  "password": "wrong-password"
					}
					"""))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.title").value("Authentication failed"));
	}

	@Test
	void shouldBlockInactiveUser() throws Exception {
		insertUser("INACTIVE", "user@assetdock.dev", "S3curePass!");

		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "email": "user@assetdock.dev",
					  "password": "S3curePass!"
					}
					"""))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.type").value("urn:assetdock:problem:user-inactive"));
	}

	@Test
	void shouldBlockLockedUser() throws Exception {
		insertUser("LOCKED", "user@assetdock.dev", "S3curePass!");

		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "email": "user@assetdock.dev",
					  "password": "S3curePass!"
					}
					"""))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.type").value("urn:assetdock:problem:user-locked"));
	}

	private void insertUser(String status, String email, String rawPassword) {
		jdbcTemplate.update(
			"""
				INSERT INTO users (id, organization_id, email, full_name, password_hash, status)
				VALUES (?, ?, ?, ?, ?, ?::user_status)
				""",
			USER_ID,
			ORGANIZATION_ID,
			email,
			"AssetDock User",
			passwordEncoder.encode(rawPassword),
			status
		);

		jdbcTemplate.update(
			"""
				INSERT INTO user_roles (user_id, role)
				VALUES (?, ?::user_role)
				""",
			USER_ID,
			"ORG_ADMIN"
		);
	}

	private void cleanDatabase() {
		jdbcTemplate.update("DELETE FROM user_roles");
		jdbcTemplate.update("DELETE FROM users");
		jdbcTemplate.update("DELETE FROM organizations");
	}
}
