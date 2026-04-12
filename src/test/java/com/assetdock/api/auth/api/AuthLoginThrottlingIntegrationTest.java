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

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
	"security.throttle.enabled=true",
	"security.throttle.login.enabled=true",
	"security.throttle.login.max-requests=2",
	"security.throttle.login.window=PT10M",
	"security.throttle.asset-import.enabled=false",
	"security.throttle.asset-import.max-requests=100",
	"security.auth.max-failed-login-attempts=10"
})
class AuthLoginThrottlingIntegrationTest extends AbstractIntegrationTest {

	private static final UUID ORGANIZATION_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
	private static final UUID USER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
	private static final String CLIENT_IP = "203.0.113.10";

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
		insertUser("user@assetdock.dev", "S3curePass!");
	}

	@AfterEach
	void tearDown() {
		cleanDatabase();
	}

	@Test
	void shouldThrottleRepeatedLoginAttemptsFromSameClient() throws Exception {
		performInvalidLoginAttempt()
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.title").value("Authentication failed"));

		performInvalidLoginAttempt()
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.title").value("Authentication failed"));

		performInvalidLoginAttempt()
			.andExpect(status().isTooManyRequests())
			.andExpect(header().exists("Retry-After"))
			.andExpect(jsonPath("$.type").value("urn:assetdock:problem:rate-limit-exceeded"))
			.andExpect(jsonPath("$.detail").value("Too many requests. Please retry later."));

		org.assertj.core.api.Assertions.assertThat(currentFailedLoginAttempts()).isEqualTo(2);
	}

	private org.springframework.test.web.servlet.ResultActions performInvalidLoginAttempt() throws Exception {
		return mockMvc.perform(post("/api/v1/auth/login")
			.with(request -> {
				request.setRemoteAddr(CLIENT_IP);
				return request;
			})
			.contentType(APPLICATION_JSON)
			.content("""
				{
				  "email": "user@assetdock.dev",
				  "password": "wrong-password"
				}
				"""));
	}

	private void insertUser(String email, String rawPassword) {
		jdbcTemplate.update(
			"""
				INSERT INTO users (id, organization_id, email, full_name, password_hash, status, failed_login_attempts)
				VALUES (?, ?, ?, ?, ?, 'ACTIVE'::user_status, 0)
				""",
			USER_ID,
			ORGANIZATION_ID,
			email,
			"AssetDock User",
			passwordEncoder.encode(rawPassword)
		);

		jdbcTemplate.update(
			"""
				INSERT INTO user_roles (user_id, role)
				VALUES (?, 'ORG_ADMIN'::user_role)
				""",
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

	private void cleanDatabase() {
		jdbcTemplate.update("DELETE FROM audit_logs");
		jdbcTemplate.update("DELETE FROM user_roles");
		jdbcTemplate.update("DELETE FROM users");
		jdbcTemplate.update("DELETE FROM organizations");
	}
}
