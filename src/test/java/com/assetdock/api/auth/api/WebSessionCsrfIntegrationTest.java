package com.assetdock.api.auth.api;
import com.assetdock.api.support.AbstractIntegrationTest;

import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static com.assetdock.api.support.MockMvcClientIp.uniqueClientIp;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WebSessionCsrfIntegrationTest extends AbstractIntegrationTest {

	private static final UUID ORGANIZATION_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
	private static final UUID USER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
	private static final UUID ASSET_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

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
		jdbcTemplate.update(
			"""
				INSERT INTO assets (
					id, organization_id, asset_tag, display_name, status, created_at, updated_at
				) VALUES (?, ?, ?, ?, ?::asset_status, now(), now())
				""",
			ASSET_ID,
			ORGANIZATION_ID,
			"AST-1",
			"Asset One",
			"IN_STOCK"
		);
	}

	@AfterEach
	void tearDown() {
		cleanDatabase();
	}

	@Test
	void mutationProtectedByWebSessionRejectsMissingCsrfToken() throws Exception {
		MvcResult loginResult = performWebLogin();
		Cookie sessionCookie = loginResult.getResponse().getCookie("assetdock_session");
		Cookie csrfCookie = loginResult.getResponse().getCookie("assetdock_csrf");
		assertThat(sessionCookie).isNotNull();
		assertThat(csrfCookie).isNotNull();

		mockMvc.perform(patch("/assets/{id}/status", ASSET_ID)
				.cookie(sessionCookie, csrfCookie)
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "status": "RETIRED"
					}
					"""))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.type").value("urn:assetdock:problem:access-denied"));
	}

	@Test
	void mutationProtectedByWebSessionAcceptsValidCsrfToken() throws Exception {
		MvcResult loginResult = performWebLogin();
		Cookie sessionCookie = loginResult.getResponse().getCookie("assetdock_session");
		Cookie csrfCookie = loginResult.getResponse().getCookie("assetdock_csrf");
		assertThat(sessionCookie).isNotNull();
		assertThat(csrfCookie).isNotNull();

		mockMvc.perform(patch("/assets/{id}/status", ASSET_ID)
				.cookie(sessionCookie, csrfCookie)
				.header("X-CSRF-Token", csrfCookie.getValue())
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "status": "RETIRED"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("RETIRED"));
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
			"Asset Manager",
			passwordEncoder.encode(rawPassword)
		);
		jdbcTemplate.update(
			"""
				INSERT INTO user_roles (user_id, role)
				VALUES (?, 'ASSET_MANAGER'::user_role)
				""",
			USER_ID
		);
	}

	private void cleanDatabase() {
		jdbcTemplate.update("""
			TRUNCATE TABLE
			    web_sessions,
			    audit_logs,
			    asset_assignments,
			    asset_import_jobs,
			    assets,
			    user_roles,
			    users,
			    organizations
			CASCADE
			""");
	}
}
