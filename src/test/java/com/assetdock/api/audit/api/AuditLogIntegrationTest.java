package com.assetdock.api.audit.api;

import java.util.Map;
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

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class AuditLogIntegrationTest {

	private static final UUID ORG_1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID ORG_ADMIN_1 = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
	private static final UUID TARGET_USER_1 = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");

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
		insertOrganization(ORG_1, "org-one");
		insertUser(ORG_ADMIN_1, ORG_1, "orgadmin1@assetdock.dev", "ORG_ADMIN");
		insertUser(TARGET_USER_1, ORG_1, "target1@assetdock.dev", "VIEWER");
	}

	@AfterEach
	void tearDown() {
		cleanDatabase();
	}

	@Test
	void successfulLoginShouldPersistAuditEvent() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "email": "orgadmin1@assetdock.dev",
					  "password": "S3curePass!"
					}
					"""))
			.andExpect(status().isOk());

		Map<String, Object> event = latestAuditEvent();
		org.assertj.core.api.Assertions.assertThat(event)
			.containsEntry("event_type", "LOGIN_SUCCESS")
			.containsEntry("outcome", "SUCCESS")
			.containsEntry("organization_id", ORG_1)
			.containsEntry("actor_user_id", ORG_ADMIN_1)
			.containsEntry("resource_id", ORG_ADMIN_1);
	}

	@Test
	void invalidLoginShouldPersistAuditEvent() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "email": "orgadmin1@assetdock.dev",
					  "password": "wrong-password"
					}
					"""))
			.andExpect(status().isUnauthorized());

		Map<String, Object> event = latestAuditEvent();
		org.assertj.core.api.Assertions.assertThat(event)
			.containsEntry("event_type", "LOGIN_FAILURE")
			.containsEntry("outcome", "FAILURE")
			.containsEntry("organization_id", ORG_1)
			.containsEntry("resource_id", ORG_ADMIN_1);
	}

	@Test
	void createUserShouldPersistAuditEvent() throws Exception {
		String token = login("orgadmin1@assetdock.dev", "S3curePass!");

		mockMvc.perform(post("/users")
				.header(AUTHORIZATION, bearer(token))
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "fullName": "Created User",
					  "email": "created.user@assetdock.dev",
					  "password": "S3curePass!",
					  "roles": ["VIEWER"],
					  "status": "ACTIVE"
					}
					"""))
			.andExpect(status().isCreated());

		Map<String, Object> event = latestAuditEvent();
		org.assertj.core.api.Assertions.assertThat(event)
			.containsEntry("event_type", "USER_CREATED")
			.containsEntry("outcome", "SUCCESS")
			.containsEntry("organization_id", ORG_1)
			.containsEntry("actor_user_id", ORG_ADMIN_1);
	}

	@Test
	void updateUserStatusShouldPersistAuditEvent() throws Exception {
		String token = login("orgadmin1@assetdock.dev", "S3curePass!");

		mockMvc.perform(patch("/users/{id}/status", TARGET_USER_1)
				.header(AUTHORIZATION, bearer(token))
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "status": "LOCKED"
					}
					"""))
			.andExpect(status().isOk());

		Map<String, Object> event = latestAuditEvent();
		org.assertj.core.api.Assertions.assertThat(event)
			.containsEntry("event_type", "USER_DISABLED")
			.containsEntry("outcome", "SUCCESS")
			.containsEntry("organization_id", ORG_1)
			.containsEntry("actor_user_id", ORG_ADMIN_1)
			.containsEntry("resource_id", TARGET_USER_1);
	}

	private Map<String, Object> latestAuditEvent() {
		return jdbcTemplate.queryForMap("""
			SELECT event_type, outcome, organization_id, actor_user_id, resource_id
			FROM audit_logs
			ORDER BY occurred_at DESC
			LIMIT 1
			""");
	}

	private String login(String email, String password) throws Exception {
		String response = mockMvc.perform(post("/api/v1/auth/login")
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "email": "%s",
					  "password": "%s"
					}
					""".formatted(email, password)))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();

		int start = response.indexOf("\"accessToken\":\"") + 15;
		int end = response.indexOf('"', start);
		return response.substring(start, end);
	}

	private String bearer(String token) {
		return "Bearer " + token;
	}

	private void insertOrganization(UUID organizationId, String slug) {
		jdbcTemplate.update(
			"INSERT INTO organizations (id, name, slug) VALUES (?, ?, ?)",
			organizationId,
			slug,
			slug
		);
	}

	private void insertUser(UUID userId, UUID organizationId, String email, String role) {
		jdbcTemplate.update(
			"""
				INSERT INTO users (id, organization_id, email, full_name, password_hash, status)
				VALUES (?, ?, ?, ?, ?, ?::user_status)
				""",
			userId,
			organizationId,
			email,
			email,
			passwordEncoder.encode("S3curePass!"),
			"ACTIVE"
		);

		jdbcTemplate.update(
			"""
				INSERT INTO user_roles (user_id, role)
				VALUES (?, ?::user_role)
				""",
			userId,
			role
		);
	}

	private void cleanDatabase() {
		jdbcTemplate.update("DELETE FROM audit_logs");
		jdbcTemplate.update("DELETE FROM user_roles");
		jdbcTemplate.update("DELETE FROM users");
		jdbcTemplate.update("DELETE FROM organizations");
	}
}
