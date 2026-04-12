package com.assetdock.api.user.api;
import com.assetdock.api.support.AbstractIntegrationTest;

import com.assetdock.api.auth.infrastructure.JwtTokenService;
import com.assetdock.api.security.auth.AuthenticatedUserPrincipal;
import com.assetdock.api.user.domain.UserRole;
import com.assetdock.api.user.domain.UserStatus;
import java.util.UUID;
import java.util.stream.Stream;
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
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserManagementIntegrationTest extends AbstractIntegrationTest {

	private static final UUID ORG_1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID ORG_2 = UUID.fromString("22222222-2222-2222-2222-222222222222");
	private static final UUID ORG_ADMIN_1 = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
	private static final UUID AUDITOR_1 = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
	private static final UUID VIEWER_1 = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
	private static final UUID ASSET_MANAGER_1 = UUID.fromString("12121212-1212-1212-1212-121212121212");
	private static final UUID ORG_ADMIN_2 = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
	private static final UUID TARGET_USER_1 = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
	private static final UUID TARGET_USER_2 = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");

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
		insertUser(ORG_ADMIN_2, ORG_2, "orgadmin2@assetdock.dev", "ORG_ADMIN");
		insertUser(TARGET_USER_1, ORG_1, "target1@assetdock.dev", "VIEWER");
		insertUser(TARGET_USER_2, ORG_2, "target2@assetdock.dev", "VIEWER");
	}

	@AfterEach
	void tearDown() {
		cleanDatabase();
	}

	@Test
	void orgAdminShouldCreateUserInOwnOrganization() throws Exception {
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
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.organizationId").value(ORG_1.toString()))
			.andExpect(jsonPath("$.email").value("created.user@assetdock.dev"))
			.andExpect(jsonPath("$.roles[0]").value("VIEWER"));
	}

	@Test
	void orgAdminShouldUpdateRolesInOwnOrganization() throws Exception {
		String token = login("orgadmin1@assetdock.dev", "S3curePass!");

		mockMvc.perform(patch("/users/{id}/roles", TARGET_USER_1)
				.header(AUTHORIZATION, bearer(token))
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "roles": ["ASSET_MANAGER", "AUDITOR"]
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.roles[*]").value(containsInAnyOrder("ASSET_MANAGER", "AUDITOR")));

		String eventType = jdbcTemplate.queryForObject(
			"""
				SELECT event_type
				FROM audit_logs
				WHERE resource_id = ?
				ORDER BY occurred_at DESC
				LIMIT 1
				""",
			String.class,
			TARGET_USER_1
		);
		org.assertj.core.api.Assertions.assertThat(eventType).isEqualTo("USER_ROLES_UPDATED");
	}

	@Test
	void shouldDenyCrossTenantRead() throws Exception {
		String token = login("orgadmin1@assetdock.dev", "S3curePass!");

		mockMvc.perform(get("/users/{id}", TARGET_USER_2)
				.header(AUTHORIZATION, bearer(token)))
			.andExpect(status().isForbidden());
	}

	@Test
	void shouldDenyCrossTenantUpdate() throws Exception {
		String token = login("orgadmin1@assetdock.dev", "S3curePass!");

		mockMvc.perform(patch("/users/{id}/status", TARGET_USER_2)
				.header(AUTHORIZATION, bearer(token))
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "status": "LOCKED"
					}
					"""))
			.andExpect(status().isForbidden());
	}

	@Test
	void shouldBlockSelfDemotionOfLastEffectiveOrgAdmin() throws Exception {
		String token = login("orgadmin1@assetdock.dev", "S3curePass!");

		mockMvc.perform(patch("/users/{id}/roles", ORG_ADMIN_1)
				.header(AUTHORIZATION, bearer(token))
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "roles": ["VIEWER"]
					}
					"""))
			.andExpect(status().isBadRequest());
	}

	@Test
	void shouldBlockSelfLockOfLastEffectiveOrgAdmin() throws Exception {
		String token = login("orgadmin1@assetdock.dev", "S3curePass!");

		mockMvc.perform(patch("/users/{id}/status", ORG_ADMIN_1)
				.header(AUTHORIZATION, bearer(token))
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "status": "LOCKED"
					}
					"""))
			.andExpect(status().isBadRequest());
	}

	@Test
	void shouldBlockSelfDisableOfLastEffectiveOrgAdmin() throws Exception {
		String token = login("orgadmin1@assetdock.dev", "S3curePass!");

		mockMvc.perform(patch("/users/{id}/status", ORG_ADMIN_1)
				.header(AUTHORIZATION, bearer(token))
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "status": "INACTIVE"
					}
					"""))
			.andExpect(status().isBadRequest());
	}

	@Test
	void shouldDenyTenantPrivilegeEscalationToSuperAdmin() throws Exception {
		String token = login("orgadmin1@assetdock.dev", "S3curePass!");

		mockMvc.perform(patch("/users/{id}/roles", TARGET_USER_1)
				.header(AUTHORIZATION, bearer(token))
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "roles": ["SUPER_ADMIN"]
					}
					"""))
			.andExpect(status().isForbidden());
	}

	@Test
	void shouldUnlockUserAndPersistAuditEvent() throws Exception {
		UUID lockedUserId = UUID.fromString("56565656-5656-5656-5656-565656565656");
		insertUser(
			lockedUserId,
			ORG_1,
			"locked.user@assetdock.dev",
			UserStatus.LOCKED,
			"VIEWER"
		);

		String token = login("orgadmin1@assetdock.dev", "S3curePass!");

		mockMvc.perform(patch("/users/{id}/status", lockedUserId)
				.header(AUTHORIZATION, bearer(token))
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "status": "ACTIVE"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ACTIVE"));

		String eventType = jdbcTemplate.queryForObject(
			"""
				SELECT event_type
				FROM audit_logs
				WHERE resource_id = ?
				ORDER BY occurred_at DESC
				LIMIT 1
			""",
			String.class,
			lockedUserId
		);
		org.assertj.core.api.Assertions.assertThat(eventType).isEqualTo("USER_UNLOCKED");
	}

	@Test
	void shouldReactivateUserAndPersistAuditEvent() throws Exception {
		insertUser(
			UUID.fromString("34343434-3434-3434-3434-343434343434"),
			ORG_1,
			"inactive.user@assetdock.dev",
			UserStatus.INACTIVE,
			"VIEWER"
		);

		String token = login("orgadmin1@assetdock.dev", "S3curePass!");

		mockMvc.perform(patch("/users/{id}/status", UUID.fromString("34343434-3434-3434-3434-343434343434"))
				.header(AUTHORIZATION, bearer(token))
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "status": "ACTIVE"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ACTIVE"));

		String eventType = jdbcTemplate.queryForObject(
			"""
				SELECT event_type
				FROM audit_logs
				WHERE resource_id = ?
				ORDER BY occurred_at DESC
				LIMIT 1
				""",
			String.class,
			UUID.fromString("34343434-3434-3434-3434-343434343434")
		);
		org.assertj.core.api.Assertions.assertThat(eventType).isEqualTo("USER_REACTIVATED");
	}

	@Test
	void auditorShouldReadLimitedPayload() throws Exception {
		String token = login("auditor1@assetdock.dev", "S3curePass!");

		mockMvc.perform(get("/users/{id}", TARGET_USER_1)
				.header(AUTHORIZATION, bearer(token)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(TARGET_USER_1.toString()))
			.andExpect(jsonPath("$.email").value("target1@assetdock.dev"))
			.andExpect(jsonPath("$.organizationId").doesNotExist())
			.andExpect(jsonPath("$.createdAt").doesNotExist())
			.andExpect(jsonPath("$.updatedAt").doesNotExist());
	}

	@Test
	void viewerShouldNotMutateUsers() throws Exception {
		String token = login("viewer1@assetdock.dev", "S3curePass!");

		mockMvc.perform(post("/users")
				.header(AUTHORIZATION, bearer(token))
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "fullName": "Blocked User",
					  "email": "blocked.user@assetdock.dev",
					  "password": "S3curePass!",
					  "roles": ["VIEWER"],
					  "status": "ACTIVE"
					}
					"""))
			.andExpect(status().isForbidden());
	}

	@Test
	void assetManagerShouldNotManageUsersOrRoles() throws Exception {
		String token = login("manager1@assetdock.dev", "S3curePass!");

		mockMvc.perform(post("/users")
				.header(AUTHORIZATION, bearer(token))
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "fullName": "Blocked Manager Action",
					  "email": "blocked.manager@assetdock.dev",
					  "password": "S3curePass!",
					  "roles": ["VIEWER"],
					  "status": "ACTIVE"
					}
					"""))
			.andExpect(status().isForbidden());

		mockMvc.perform(patch("/users/{id}/roles", TARGET_USER_1)
				.header(AUTHORIZATION, bearer(token))
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "roles": ["AUDITOR"]
					}
					"""))
			.andExpect(status().isForbidden());
	}

	@Test
	void userListShouldBeBoundedToHundredItems() throws Exception {
		for (int index = 0; index < 110; index++) {
			insertUser(
				UUID.fromString("90000000-0000-0000-0000-%012d".formatted(index + 1)),
				ORG_1,
				"bulk-user-%03d@assetdock.dev".formatted(index),
				"VIEWER"
			);
		}

		String token = login("orgadmin1@assetdock.dev", "S3curePass!");

		mockMvc.perform(get("/users")
				.header(AUTHORIZATION, bearer(token)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(100)));
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
		insertUser(userId, organizationId, email, UserStatus.ACTIVE, role);
	}

	private void insertUser(UUID userId, UUID organizationId, String email, UserStatus status, String... roles) {
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
			status.name()
		);

		Stream.of(roles).forEach(role -> jdbcTemplate.update(
			"""
				INSERT INTO user_roles (user_id, role)
				VALUES (?, ?::user_role)
				""",
			userId,
			role
		));
	}

	private void cleanDatabase() {
		jdbcTemplate.update("DELETE FROM audit_logs");
		jdbcTemplate.update("DELETE FROM user_roles");
		jdbcTemplate.update("DELETE FROM users");
		jdbcTemplate.update("DELETE FROM organizations");
	}
}
