package com.assetdock.api.auth.api;

import com.assetdock.api.support.AbstractIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static com.assetdock.api.support.MockMvcClientIp.uniqueClientIp;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
	"security.auth.max-failed-login-attempts=3"
})
class AuthLoginIntegrationTest extends AbstractIntegrationTest {

	private static final UUID ORGANIZATION_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
	private static final UUID USER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private PasswordEncoder passwordEncoder;

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
				.with(uniqueClientIp())
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
				.with(uniqueClientIp())
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
	void shouldAutomaticallyLockUserAfterConfiguredFailedLoginThreshold() throws Exception {
		insertUser("ACTIVE", "user@assetdock.dev", "S3curePass!", 2);

		mockMvc.perform(post("/api/v1/auth/login")
				.with(uniqueClientIp())
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "email": "user@assetdock.dev",
					  "password": "wrong-password"
					}
					"""))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.title").value("Authentication failed"));

		org.assertj.core.api.Assertions.assertThat(currentUserStatus()).isEqualTo("LOCKED");
		org.assertj.core.api.Assertions.assertThat(currentFailedLoginAttempts()).isEqualTo(3);
		org.assertj.core.api.Assertions.assertThat(auditEventCount("USER_LOCKED")).isEqualTo(1);
	}

	@Test
	void shouldBlockInactiveUser() throws Exception {
		insertUser("INACTIVE", "user@assetdock.dev", "S3curePass!");

		mockMvc.perform(post("/api/v1/auth/login")
				.with(uniqueClientIp())
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
				.with(uniqueClientIp())
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

	@Test
	void shouldResetFailedLoginAttemptsAfterSuccessfulAuthentication() throws Exception {
		insertUser("ACTIVE", "user@assetdock.dev", "S3curePass!", 2);

		mockMvc.perform(post("/api/v1/auth/login")
				.with(uniqueClientIp())
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "email": "user@assetdock.dev",
					  "password": "S3curePass!"
					}
					"""))
			.andExpect(status().isOk());

		org.assertj.core.api.Assertions.assertThat(currentUserStatus()).isEqualTo("ACTIVE");
		org.assertj.core.api.Assertions.assertThat(currentFailedLoginAttempts()).isZero();
		org.assertj.core.api.Assertions.assertThat(auditEventCount("LOGIN_SUCCESS")).isEqualTo(1);
		org.assertj.core.api.Assertions.assertThat(loginSuccessResetAuditCount()).isEqualTo(1);
	}

	private void insertUser(String status, String email, String rawPassword) {
		insertUser(status, email, rawPassword, 0);
	}

	private void insertUser(String status, String email, String rawPassword, int failedLoginAttempts) {
		jdbcTemplate.update(
			"""
				INSERT INTO users (id, organization_id, email, full_name, password_hash, status, failed_login_attempts)
				VALUES (?, ?, ?, ?, ?, ?::user_status, ?)
				""",
			USER_ID,
			ORGANIZATION_ID,
			email,
			"AssetDock User",
			passwordEncoder.encode(rawPassword),
			status,
			failedLoginAttempts
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
		jdbcTemplate.update("DELETE FROM audit_logs");
		jdbcTemplate.update("DELETE FROM user_roles");
		jdbcTemplate.update("DELETE FROM users");
		jdbcTemplate.update("DELETE FROM organizations");
	}

	private String currentUserStatus() {
		return jdbcTemplate.queryForObject(
			"SELECT status::text FROM users WHERE id = ?",
			String.class,
			USER_ID
		);
	}

	private int currentFailedLoginAttempts() {
		Integer value = jdbcTemplate.queryForObject(
			"SELECT failed_login_attempts FROM users WHERE id = ?",
			Integer.class,
			USER_ID
		);
		return value == null ? -1 : value;
	}

	private int auditEventCount(String eventType) {
		Long value = jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM audit_logs WHERE event_type = ?::audit_event_type",
			Long.class,
			eventType
		);
		return value == null ? 0 : value.intValue();
	}

	private int loginSuccessResetAuditCount() {
		Long value = jdbcTemplate.queryForObject(
			"""
				SELECT COUNT(*)
				FROM audit_logs
				WHERE event_type = 'LOGIN_SUCCESS'::audit_event_type
				  AND details_json ->> 'failedLoginAttemptsReset' = 'true'
				""",
			Long.class
		);
		return value == null ? 0 : value.intValue();
	}
}
