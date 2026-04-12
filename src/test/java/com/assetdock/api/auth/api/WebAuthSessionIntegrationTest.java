package com.assetdock.api.auth.api;
import com.assetdock.api.support.AbstractIntegrationTest;

import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static com.assetdock.api.support.MockMvcClientIp.uniqueClientIp;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.HttpHeaders.SET_COOKIE;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WebAuthSessionIntegrationTest extends AbstractIntegrationTest {

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
		insertUser("user@assetdock.dev", "S3curePass!");
	}

	@AfterEach
	void tearDown() {
		cleanDatabase();
	}

	@Test
	void webLoginCreatesSessionAndResolvesCurrentUser() throws Exception {
		MvcResult loginResult = performWebLogin();
		MockHttpServletResponse response = loginResult.getResponse();
		List<String> setCookies = response.getHeaders(SET_COOKIE);

		assertThat(setCookies).anyMatch(value -> value.contains("assetdock_session=") && value.contains("HttpOnly"));
		assertThat(setCookies).anyMatch(value -> value.contains("assetdock_csrf=") && !value.contains("HttpOnly"));

		Cookie sessionCookie = response.getCookie("assetdock_session");
		Cookie csrfCookie = response.getCookie("assetdock_csrf");
		assertThat(sessionCookie).isNotNull();
		assertThat(csrfCookie).isNotNull();
		assertThat(webSessionCount()).isEqualTo(1);
		assertThat(auditEventCount("WEB_SESSION_CREATED")).isEqualTo(1);

		mockMvc.perform(get("/api/v1/web/auth/me")
				.cookie(sessionCookie))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.user.id").value(USER_ID.toString()))
			.andExpect(jsonPath("$.user.fullName").value("AssetDock User"))
			.andExpect(jsonPath("$.user.email").value("user@assetdock.dev"))
			.andExpect(jsonPath("$.user.role").value("ORG_ADMIN"))
			.andExpect(jsonPath("$.user.organizationId").value(ORGANIZATION_ID.toString()));
	}

	@Test
	void webAuthEndpointsRejectRequestsWithoutSession() throws Exception {
		mockMvc.perform(get("/api/v1/web/auth/me"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.type").value("urn:assetdock:problem:authentication-required"));
	}

	@Test
	void logoutInvalidatesSessionAndClearsCookies() throws Exception {
		MvcResult loginResult = performWebLogin();
		Cookie sessionCookie = loginResult.getResponse().getCookie("assetdock_session");
		Cookie csrfCookie = loginResult.getResponse().getCookie("assetdock_csrf");
		assertThat(sessionCookie).isNotNull();
		assertThat(csrfCookie).isNotNull();

		mockMvc.perform(post("/api/v1/web/auth/logout")
				.cookie(sessionCookie, csrfCookie)
				.header("X-CSRF-Token", csrfCookie.getValue()))
			.andExpect(status().isNoContent())
			.andExpect(cookie().maxAge("assetdock_session", 0))
			.andExpect(cookie().maxAge("assetdock_csrf", 0))
			.andExpect(cookie().value("assetdock_session", ""))
			.andExpect(cookie().value("assetdock_csrf", ""));

		assertThat(invalidatedWebSessionCount()).isEqualTo(1);
		assertThat(auditEventCount("WEB_SESSION_LOGGED_OUT")).isEqualTo(1);

		mockMvc.perform(get("/api/v1/web/auth/me")
				.cookie(sessionCookie))
			.andExpect(status().isUnauthorized());
	}

	private MvcResult performWebLogin() throws Exception {
		return mockMvc.perform(post("/api/v1/web/auth/login")
				.with(uniqueClientIp())
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "email": "user@assetdock.dev",
					  "password": "S3curePass!"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(header().string(SET_COOKIE, containsString("assetdock_session=")))
			.andReturn();
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

	private long webSessionCount() {
		Long value = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM web_sessions", Long.class);
		return value == null ? 0 : value;
	}

	private long invalidatedWebSessionCount() {
		Long value = jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM web_sessions WHERE invalidated_at IS NOT NULL",
			Long.class
		);
		return value == null ? 0 : value;
	}

	private int auditEventCount(String eventType) {
		Long value = jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM audit_logs WHERE event_type = ?::audit_event_type",
			Long.class,
			eventType
		);
		return value == null ? 0 : value.intValue();
	}

	private void cleanDatabase() {
		jdbcTemplate.update("DELETE FROM web_sessions");
		jdbcTemplate.update("DELETE FROM audit_logs");
		jdbcTemplate.update("DELETE FROM user_roles");
		jdbcTemplate.update("DELETE FROM users");
		jdbcTemplate.update("DELETE FROM organizations");
	}
}
