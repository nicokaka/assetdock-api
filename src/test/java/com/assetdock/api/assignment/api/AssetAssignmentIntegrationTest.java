package com.assetdock.api.assignment.api;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class AssetAssignmentIntegrationTest {

	private static final UUID ORG_1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID ORG_2 = UUID.fromString("22222222-2222-2222-2222-222222222222");
	private static final UUID ORG_ADMIN_1 = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
	private static final UUID ASSET_MANAGER_1 = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
	private static final UUID AUDITOR_1 = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
	private static final UUID VIEWER_1 = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
	private static final UUID USER_1 = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
	private static final UUID USER_2 = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
	private static final UUID USER_INACTIVE_1 = UUID.fromString("11111111-2222-3333-4444-555555555555");
	private static final UUID USER_LOCKED_1 = UUID.fromString("66666666-7777-8888-9999-000000000000");
	private static final UUID LOCATION_1 = UUID.fromString("10000000-0000-0000-0000-000000000001");
	private static final UUID LOCATION_2 = UUID.fromString("20000000-0000-0000-0000-000000000002");
	private static final UUID LOCATION_INACTIVE_1 = UUID.fromString("21000000-0000-0000-0000-000000000002");
	private static final UUID CATEGORY_1 = UUID.fromString("30000000-0000-0000-0000-000000000003");
	private static final UUID MANUFACTURER_1 = UUID.fromString("40000000-0000-0000-0000-000000000004");
	private static final UUID ASSET_AVAILABLE_1 = UUID.fromString("50000000-0000-0000-0000-000000000005");
	private static final UUID ASSET_ASSIGNED_1 = UUID.fromString("60000000-0000-0000-0000-000000000006");
	private static final UUID ASSET_RETIRED_1 = UUID.fromString("70000000-0000-0000-0000-000000000007");
	private static final UUID ASSET_LOST_1 = UUID.fromString("80000000-0000-0000-0000-000000000008");
	private static final UUID ASSET_ARCHIVED_1 = UUID.fromString("81000000-0000-0000-0000-000000000008");
	private static final UUID ASSET_ORG_2 = UUID.fromString("90000000-0000-0000-0000-000000000009");
	private static final UUID ACTIVE_ASSIGNMENT_1 = UUID.fromString("a0000000-0000-0000-0000-00000000000a");

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
		insertOrganization(ORG_2, "org-two");

		insertUser(ORG_ADMIN_1, ORG_1, "orgadmin1@assetdock.dev", "ORG_ADMIN");
		insertUser(ASSET_MANAGER_1, ORG_1, "manager1@assetdock.dev", "ASSET_MANAGER");
		insertUser(AUDITOR_1, ORG_1, "auditor1@assetdock.dev", "AUDITOR");
		insertUser(VIEWER_1, ORG_1, "viewer1@assetdock.dev", "VIEWER");
		insertUser(USER_1, ORG_1, "user1@assetdock.dev", "VIEWER");
		insertUser(USER_2, ORG_2, "user2@assetdock.dev", "VIEWER");
		insertUser(USER_INACTIVE_1, ORG_1, "inactive1@assetdock.dev", "VIEWER", "INACTIVE");
		insertUser(USER_LOCKED_1, ORG_1, "locked1@assetdock.dev", "VIEWER", "LOCKED");

		insertCategory(CATEGORY_1, ORG_1, "Laptops");
		insertManufacturer(MANUFACTURER_1, ORG_1, "Lenovo");
		insertLocation(LOCATION_1, ORG_1, "Warehouse");
		insertLocation(LOCATION_2, ORG_2, "HQ");
		insertLocation(LOCATION_INACTIVE_1, ORG_1, "Disabled site", false);

		insertAsset(ASSET_AVAILABLE_1, ORG_1, "AST-001", CATEGORY_1, MANUFACTURER_1, LOCATION_1, null, "IN_STOCK");
		insertAsset(ASSET_ASSIGNED_1, ORG_1, "AST-002", CATEGORY_1, MANUFACTURER_1, LOCATION_1, USER_1, "ASSIGNED");
		insertAsset(ASSET_RETIRED_1, ORG_1, "AST-003", CATEGORY_1, MANUFACTURER_1, LOCATION_1, null, "RETIRED");
		insertAsset(ASSET_LOST_1, ORG_1, "AST-004", CATEGORY_1, MANUFACTURER_1, LOCATION_1, null, "LOST");
		insertArchivedAsset(ASSET_ARCHIVED_1, ORG_1, "AST-005", CATEGORY_1, MANUFACTURER_1, LOCATION_1);
		insertAsset(ASSET_ORG_2, ORG_2, "AST-900", null, null, LOCATION_2, USER_2, "ASSIGNED");

		insertActiveAssignment(ACTIVE_ASSIGNMENT_1, ORG_1, ASSET_ASSIGNED_1, USER_1, LOCATION_1, ORG_ADMIN_1);
	}

	@AfterEach
	void tearDown() {
		cleanDatabase();
	}

	@Test
	void orgAdminAssignsAssetOfOwnOrganization() throws Exception {
		String token = login("orgadmin1@assetdock.dev", "S3curePass!");

		mockMvc.perform(post("/assets/{id}/assignments", ASSET_AVAILABLE_1)
				.header(AUTHORIZATION, bearer(token))
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "userId": "%s",
					  "locationId": "%s",
					  "notes": "Primary allocation"
					}
					""".formatted(USER_1, LOCATION_1)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.assetId").value(ASSET_AVAILABLE_1.toString()))
			.andExpect(jsonPath("$.userId").value(USER_1.toString()));

		Map<String, Object> snapshot = jdbcTemplate.queryForMap("""
			SELECT current_assigned_user_id, current_location_id, status
			FROM assets
			WHERE id = ?
			""", ASSET_AVAILABLE_1);
		org.assertj.core.api.Assertions.assertThat(snapshot)
			.containsEntry("current_assigned_user_id", USER_1)
			.containsEntry("current_location_id", LOCATION_1)
			.containsEntry("status", "ASSIGNED");
	}

	@Test
	void assetManagerUnassignsAssetOfOwnOrganization() throws Exception {
		String token = login("manager1@assetdock.dev", "S3curePass!");

		mockMvc.perform(post("/assets/{id}/unassign", ASSET_ASSIGNED_1)
				.header(AUTHORIZATION, bearer(token)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(ACTIVE_ASSIGNMENT_1.toString()))
			.andExpect(jsonPath("$.unassignedAt").isNotEmpty());

		Map<String, Object> snapshot = jdbcTemplate.queryForMap("""
			SELECT current_assigned_user_id, status
			FROM assets
			WHERE id = ?
			""", ASSET_ASSIGNED_1);
		org.assertj.core.api.Assertions.assertThat(snapshot)
			.containsEntry("current_assigned_user_id", null)
			.containsEntry("status", "IN_STOCK");
	}

	@Test
	void auditorListsAssignmentHistoryOfOwnOrganization() throws Exception {
		String token = login("auditor1@assetdock.dev", "S3curePass!");

		mockMvc.perform(get("/assets/{id}/assignments", ASSET_ASSIGNED_1)
				.header(AUTHORIZATION, bearer(token)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].id").value(ACTIVE_ASSIGNMENT_1.toString()));
	}

	@Test
	void viewerListsAssignmentHistoryOfOwnOrganization() throws Exception {
		String token = login("viewer1@assetdock.dev", "S3curePass!");

		mockMvc.perform(get("/assets/{id}/assignments", ASSET_ASSIGNED_1)
				.header(AUTHORIZATION, bearer(token)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].id").value(ACTIVE_ASSIGNMENT_1.toString()));
	}

	@Test
	void crossTenantAccessToAssignmentsIsDenied() throws Exception {
		String token = login("auditor1@assetdock.dev", "S3curePass!");

		mockMvc.perform(get("/assets/{id}/assignments", ASSET_ORG_2)
				.header(AUTHORIZATION, bearer(token)))
			.andExpect(status().isNotFound());
	}

	@Test
	void crossTenantUnassignIsDenied() throws Exception {
		String token = login("orgadmin1@assetdock.dev", "S3curePass!");

		mockMvc.perform(post("/assets/{id}/unassign", ASSET_ORG_2)
				.header(AUTHORIZATION, bearer(token)))
			.andExpect(status().isNotFound());
	}

	@Test
	void assignAndUnassignPersistAuditLog() throws Exception {
		String token = login("orgadmin1@assetdock.dev", "S3curePass!");

		mockMvc.perform(post("/assets/{id}/assignments", ASSET_AVAILABLE_1)
				.header(AUTHORIZATION, bearer(token))
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "userId": "%s"
					}
					""".formatted(USER_1)))
			.andExpect(status().isCreated());

		Map<String, Object> assignedEvent = latestAuditEvent();
		org.assertj.core.api.Assertions.assertThat(assignedEvent)
			.containsEntry("event_type", "ASSET_ASSIGNED")
			.containsEntry("outcome", "SUCCESS")
			.containsEntry("organization_id", ORG_1);

		mockMvc.perform(post("/assets/{id}/unassign", ASSET_AVAILABLE_1)
				.header(AUTHORIZATION, bearer(token)))
			.andExpect(status().isOk());

		Map<String, Object> unassignedEvent = latestAuditEvent();
		org.assertj.core.api.Assertions.assertThat(unassignedEvent)
			.containsEntry("event_type", "ASSET_UNASSIGNED")
			.containsEntry("outcome", "SUCCESS")
			.containsEntry("organization_id", ORG_1);
	}

	@Test
	void shouldNotAssignRetiredOrLostAssets() throws Exception {
		String token = login("orgadmin1@assetdock.dev", "S3curePass!");

		mockMvc.perform(post("/assets/{id}/assignments", ASSET_RETIRED_1)
				.header(AUTHORIZATION, bearer(token))
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "userId": "%s"
					}
					""".formatted(USER_1)))
			.andExpect(status().isBadRequest());

		mockMvc.perform(post("/assets/{id}/assignments", ASSET_LOST_1)
				.header(AUTHORIZATION, bearer(token))
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "userId": "%s"
					}
					""".formatted(USER_1)))
			.andExpect(status().isBadRequest());
	}

	@Test
	void shouldNotAssignArchivedAssets() throws Exception {
		String token = login("orgadmin1@assetdock.dev", "S3curePass!");

		mockMvc.perform(post("/assets/{id}/assignments", ASSET_ARCHIVED_1)
				.header(AUTHORIZATION, bearer(token))
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "userId": "%s"
					}
					""".formatted(USER_1)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("Archived assets cannot be assigned."));
	}

	@Test
	void shouldNotAssignInactiveOrLockedUsers() throws Exception {
		String token = login("manager1@assetdock.dev", "S3curePass!");

		mockMvc.perform(post("/assets/{id}/assignments", ASSET_AVAILABLE_1)
				.header(AUTHORIZATION, bearer(token))
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "userId": "%s"
					}
					""".formatted(USER_INACTIVE_1)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("userId must reference an ACTIVE user."));

		mockMvc.perform(post("/assets/{id}/assignments", ASSET_AVAILABLE_1)
				.header(AUTHORIZATION, bearer(token))
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "userId": "%s"
					}
					""".formatted(USER_LOCKED_1)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("userId must reference an ACTIVE user."));
	}

	@Test
	void shouldNotAssignToInactiveLocation() throws Exception {
		String token = login("manager1@assetdock.dev", "S3curePass!");

		mockMvc.perform(post("/assets/{id}/assignments", ASSET_AVAILABLE_1)
				.header(AUTHORIZATION, bearer(token))
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "userId": "%s",
					  "locationId": "%s"
					}
					""".formatted(USER_1, LOCATION_INACTIVE_1)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("locationId must reference an active location."));
	}

	@Test
	void shouldNotAssignWhenActiveAssignmentAlreadyExists() throws Exception {
		String token = login("orgadmin1@assetdock.dev", "S3curePass!");

		mockMvc.perform(post("/assets/{id}/assignments", ASSET_ASSIGNED_1)
				.header(AUTHORIZATION, bearer(token))
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "userId": "%s"
					}
					""".formatted(USER_1)))
			.andExpect(status().isConflict());
	}

	@Test
	void shouldNotUnassignWhenThereIsNoActiveAssignment() throws Exception {
		String token = login("manager1@assetdock.dev", "S3curePass!");

		mockMvc.perform(post("/assets/{id}/unassign", ASSET_AVAILABLE_1)
				.header(AUTHORIZATION, bearer(token)))
			.andExpect(status().isBadRequest());
	}

	private Map<String, Object> latestAuditEvent() {
		return jdbcTemplate.queryForMap("""
			SELECT event_type, outcome, organization_id
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
		insertUser(userId, organizationId, email, role, "ACTIVE");
	}

	private void insertUser(UUID userId, UUID organizationId, String email, String role, String status) {
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
			status
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

	private void insertCategory(UUID id, UUID organizationId, String name) {
		jdbcTemplate.update(
			"""
				INSERT INTO categories (id, organization_id, name, active, created_at, updated_at)
				VALUES (?, ?, ?, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""",
			id,
			organizationId,
			name
		);
	}

	private void insertManufacturer(UUID id, UUID organizationId, String name) {
		jdbcTemplate.update(
			"""
				INSERT INTO manufacturers (id, organization_id, name, active, created_at, updated_at)
				VALUES (?, ?, ?, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""",
			id,
			organizationId,
			name
		);
	}

	private void insertLocation(UUID id, UUID organizationId, String name) {
		insertLocation(id, organizationId, name, true);
	}

	private void insertLocation(UUID id, UUID organizationId, String name, boolean active) {
		jdbcTemplate.update(
			"""
				INSERT INTO locations (id, organization_id, name, active, created_at, updated_at)
				VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""",
			id,
			organizationId,
			name,
			active
		);
	}

	private void insertAsset(
		UUID id,
		UUID organizationId,
		String assetTag,
		UUID categoryId,
		UUID manufacturerId,
		UUID locationId,
		UUID currentAssignedUserId,
		String status
	) {
		insertAsset(id, organizationId, assetTag, categoryId, manufacturerId, locationId, currentAssignedUserId, status, null);
	}

	private void insertArchivedAsset(
		UUID id,
		UUID organizationId,
		String assetTag,
		UUID categoryId,
		UUID manufacturerId,
		UUID locationId
	) {
		insertAsset(id, organizationId, assetTag, categoryId, manufacturerId, locationId, null, "IN_STOCK", java.time.Instant.parse("2026-03-28T10:00:00Z"));
	}

	private void insertAsset(
		UUID id,
		UUID organizationId,
		String assetTag,
		UUID categoryId,
		UUID manufacturerId,
		UUID locationId,
		UUID currentAssignedUserId,
		String status,
		java.time.Instant archivedAt
	) {
		jdbcTemplate.update(
			"""
				INSERT INTO assets (
					id,
					organization_id,
					asset_tag,
					display_name,
					category_id,
					manufacturer_id,
					current_location_id,
					current_assigned_user_id,
					status,
					archived_at,
					created_at,
					updated_at
				)
				VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::asset_status, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""",
			id,
			organizationId,
			assetTag,
			assetTag,
			categoryId,
			manufacturerId,
			locationId,
			currentAssignedUserId,
			status,
			archivedAt
		);
	}

	private void insertActiveAssignment(
		UUID id,
		UUID organizationId,
		UUID assetId,
		UUID userId,
		UUID locationId,
		UUID assignedBy
	) {
		jdbcTemplate.update(
			"""
				INSERT INTO asset_assignments (
					id,
					organization_id,
					asset_id,
					user_id,
					location_id,
					assigned_at,
					assigned_by,
					notes,
					created_at
				)
				VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, ?, ?, CURRENT_TIMESTAMP)
				""",
			id,
			organizationId,
			assetId,
			userId,
			locationId,
			assignedBy,
			"Assigned for daily use"
		);
	}

	private void cleanDatabase() {
		jdbcTemplate.update("DELETE FROM audit_logs");
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
