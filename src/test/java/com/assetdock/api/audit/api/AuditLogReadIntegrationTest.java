package com.assetdock.api.audit.api;
import com.assetdock.api.support.AbstractIntegrationTest;

import com.assetdock.api.auth.infrastructure.JwtTokenService;
import com.assetdock.api.security.auth.AuthenticatedUserPrincipal;
import com.assetdock.api.user.domain.UserRole;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static com.assetdock.api.support.MockMvcClientIp.uniqueClientIp;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuditLogReadIntegrationTest extends AbstractIntegrationTest {

	private static final UUID ORG_1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID ORG_2 = UUID.fromString("22222222-2222-2222-2222-222222222222");
	private static final UUID ORG_ADMIN_1 = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
	private static final UUID AUDITOR_1 = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
	private static final UUID VIEWER_1 = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
	private static final UUID ASSET_MANAGER_1 = UUID.fromString("abababab-abab-abab-abab-abababababab");
	private static final UUID USER_2 = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
	private static final UUID AUDIT_LOG_1 = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
	private static final UUID AUDIT_LOG_2 = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
	private static final UUID AUDIT_LOG_3 = UUID.fromString("11111111-2222-3333-4444-555555555555");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtTokenService jwtTokenService;

	@BeforeEach
	void setUp() {
		cleanDatabase();
		insertOrganization(ORG_1, "org-one");
		insertOrganization(ORG_2, "org-two");
		insertUser(ORG_ADMIN_1, ORG_1, "orgadmin1@assetdock.dev", "ORG_ADMIN");
		insertUser(AUDITOR_1, ORG_1, "auditor1@assetdock.dev", "AUDITOR");
		insertUser(VIEWER_1, ORG_1, "viewer1@assetdock.dev", "VIEWER");
		insertUser(ASSET_MANAGER_1, ORG_1, "manager1@assetdock.dev", "ASSET_MANAGER");
		insertUser(USER_2, ORG_2, "user2@assetdock.dev", "ORG_ADMIN");

		insertAuditLog(AUDIT_LOG_1, ORG_1, ORG_ADMIN_1, "LOGIN_SUCCESS", "2026-03-01T00:00:00Z");
		insertAuditLog(AUDIT_LOG_2, ORG_1, ORG_ADMIN_1, "USER_CREATED", "2026-03-02T00:00:00Z");
		insertAuditLog(AUDIT_LOG_3, ORG_2, USER_2, "LOGIN_FAILURE", "2026-03-03T00:00:00Z");
	}

	@AfterEach
	void tearDown() {
		cleanDatabase();
	}

	@Test
	void orgAdminReadsOwnOrganizationAuditLogs() throws Exception {
		String token = login("orgadmin1@assetdock.dev", "S3curePass!");

		mockMvc.perform(get("/audit-logs")
				.header(AUTHORIZATION, bearer(token)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items.length()").value(2))
			.andExpect(jsonPath("$.items[0].organizationId").value(ORG_1.toString()));
	}

	@Test
	void auditorReadsOwnOrganizationAuditLogs() throws Exception {
		String token = login("auditor1@assetdock.dev", "S3curePass!");

		mockMvc.perform(get("/audit-logs")
				.header(AUTHORIZATION, bearer(token)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items.length()").value(2));
	}

	@Test
	void viewerCannotReadAuditLogs() throws Exception {
		String token = login("viewer1@assetdock.dev", "S3curePass!");

		mockMvc.perform(get("/audit-logs")
				.header(AUTHORIZATION, bearer(token)))
			.andExpect(status().isForbidden());
	}

	@Test
	void assetManagerCannotReadAuditLogs() throws Exception {
		String token = login("manager1@assetdock.dev", "S3curePass!");

		mockMvc.perform(get("/audit-logs")
				.header(AUTHORIZATION, bearer(token)))
			.andExpect(status().isForbidden());
	}

	@Test
	void crossTenantAuditLogScopeIsDenied() throws Exception {
		String token = login("orgadmin1@assetdock.dev", "S3curePass!");

		mockMvc.perform(get("/audit-logs")
				.header(AUTHORIZATION, bearer(token))
				.param("organizationId", ORG_2.toString()))
			.andExpect(status().isForbidden());
	}

	@Test
	void auditLogPaginationWorks() throws Exception {
		String token = login("orgadmin1@assetdock.dev", "S3curePass!");

		mockMvc.perform(get("/audit-logs")
				.header(AUTHORIZATION, bearer(token))
				.param("page", "0")
				.param("size", "1"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items.length()").value(1))
			.andExpect(jsonPath("$.page").value(0))
			.andExpect(jsonPath("$.size").value(1))
			.andExpect(jsonPath("$.totalElements").value(2))
			.andExpect(jsonPath("$.totalPages").value(2));
	}

	@Test
	void auditLogSimpleFiltersWork() throws Exception {
		String token = login("orgadmin1@assetdock.dev", "S3curePass!");

		mockMvc.perform(get("/audit-logs")
				.header(AUTHORIZATION, bearer(token))
				.param("eventType", "USER_CREATED")
				.param("from", "2026-03-02T00:00:00Z")
				.param("to", "2026-03-02T23:59:59Z"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items.length()").value(1))
			.andExpect(jsonPath("$.items[0].eventType").value("USER_CREATED"));
	}

	@Test
	void auditLogQueryRejectsOversizedPageSize() throws Exception {
		String token = login("orgadmin1@assetdock.dev", "S3curePass!");

		mockMvc.perform(get("/audit-logs")
				.header(AUTHORIZATION, bearer(token))
				.param("size", "101"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.type").value("urn:assetdock:problem:invalid-audit-log-query"))
			.andExpect(jsonPath("$.detail").value("size must be between 1 and 100."));
	}

	@Test
	void auditLogQueryRejectsExcessivelyLargePageOffset() throws Exception {
		String token = login("orgadmin1@assetdock.dev", "S3curePass!");

		mockMvc.perform(get("/audit-logs")
				.header(AUTHORIZATION, bearer(token))
				.param("page", "30000000")
				.param("size", "100"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.type").value("urn:assetdock:problem:invalid-audit-log-query"))
			.andExpect(jsonPath("$.detail").value("page is too large."));
	}

	private String login(String email, String password) {
		return switch (email) {
			case "orgadmin1@assetdock.dev" -> issueToken(ORG_ADMIN_1, ORG_1, email, UserRole.ORG_ADMIN);
			case "auditor1@assetdock.dev" -> issueToken(AUDITOR_1, ORG_1, email, UserRole.AUDITOR);
			case "viewer1@assetdock.dev" -> issueToken(VIEWER_1, ORG_1, email, UserRole.VIEWER);
			case "manager1@assetdock.dev" -> issueToken(ASSET_MANAGER_1, ORG_1, email, UserRole.ASSET_MANAGER);
			default -> throw new IllegalArgumentException("Unsupported test user email: " + email);
		};
	}

	private String issueToken(UUID userId, UUID organizationId, String email, UserRole... roles) {
		return jwtTokenService.issue(
			new AuthenticatedUserPrincipal(userId, organizationId, email, java.util.Set.of(roles)),
			java.time.Instant.now()
		).value();
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

	private void insertAuditLog(
		UUID id,
		UUID organizationId,
		UUID actorUserId,
		String eventType,
		String occurredAt
	) {
		jdbcTemplate.update(
			"""
				INSERT INTO audit_logs (
					id,
					organization_id,
					actor_user_id,
					event_type,
					resource_type,
					resource_id,
					outcome,
					details_json,
					occurred_at
				)
				VALUES (?, ?, ?, ?::audit_event_type, ?, ?, ?, '{}'::jsonb, ?::timestamptz)
				""",
			id,
			organizationId,
			actorUserId,
			eventType,
			"seed",
			actorUserId,
			"SUCCESS",
			occurredAt
		);
	}

	private void cleanDatabase() {
		jdbcTemplate.update("DELETE FROM audit_logs");
		jdbcTemplate.update("DELETE FROM asset_import_jobs");
		jdbcTemplate.update("DELETE FROM asset_assignments");
		jdbcTemplate.update("DELETE FROM assets");
		jdbcTemplate.update("DELETE FROM categories");
		jdbcTemplate.update("DELETE FROM manufacturers");
		jdbcTemplate.update("DELETE FROM locations");
		jdbcTemplate.update("DELETE FROM user_roles");
		jdbcTemplate.update("DELETE FROM users");
		jdbcTemplate.update("DELETE FROM organizations");
	}
}
