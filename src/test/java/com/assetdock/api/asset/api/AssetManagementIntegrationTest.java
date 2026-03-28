package com.assetdock.api.asset.api;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class AssetManagementIntegrationTest {

	private static final UUID ORG_1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID ORG_2 = UUID.fromString("22222222-2222-2222-2222-222222222222");
	private static final UUID ORG_ADMIN_1 = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
	private static final UUID ASSET_MANAGER_1 = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
	private static final UUID AUDITOR_1 = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
	private static final UUID VIEWER_1 = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
	private static final UUID ASSIGNED_USER_1 = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
	private static final UUID FOREIGN_USER_2 = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
	private static final UUID CATEGORY_1 = UUID.fromString("10000000-0000-0000-0000-000000000001");
	private static final UUID CATEGORY_2 = UUID.fromString("20000000-0000-0000-0000-000000000002");
	private static final UUID MANUFACTURER_1 = UUID.fromString("30000000-0000-0000-0000-000000000003");
	private static final UUID MANUFACTURER_2 = UUID.fromString("40000000-0000-0000-0000-000000000004");
	private static final UUID LOCATION_1 = UUID.fromString("50000000-0000-0000-0000-000000000005");
	private static final UUID LOCATION_2 = UUID.fromString("60000000-0000-0000-0000-000000000006");
	private static final UUID ASSET_1 = UUID.fromString("70000000-0000-0000-0000-000000000007");
	private static final UUID ASSET_2 = UUID.fromString("80000000-0000-0000-0000-000000000008");

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
		insertUser(ASSIGNED_USER_1, ORG_1, "assigned1@assetdock.dev", "VIEWER");
		insertUser(FOREIGN_USER_2, ORG_2, "foreign2@assetdock.dev", "VIEWER");

		insertCategory(CATEGORY_1, ORG_1, "Laptops");
		insertCategory(CATEGORY_2, ORG_2, "Servers");
		insertManufacturer(MANUFACTURER_1, ORG_1, "Lenovo");
		insertManufacturer(MANUFACTURER_2, ORG_2, "Dell");
		insertLocation(LOCATION_1, ORG_1, "Warehouse");
		insertLocation(LOCATION_2, ORG_2, "HQ");

		insertAsset(ASSET_1, ORG_1, "AST-001", CATEGORY_1, MANUFACTURER_1, LOCATION_1, ASSIGNED_USER_1, "IN_STOCK");
		insertAsset(ASSET_2, ORG_2, "AST-900", CATEGORY_2, MANUFACTURER_2, LOCATION_2, FOREIGN_USER_2, "IN_STOCK");
	}

	@AfterEach
	void tearDown() {
		cleanDatabase();
	}

	@Test
	void orgAdminCreatesAssetOfOwnOrganization() throws Exception {
		String token = login("orgadmin1@assetdock.dev", "S3curePass!");

		mockMvc.perform(post("/assets")
				.header(AUTHORIZATION, bearer(token))
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "assetTag": "ast-002",
					  "serialNumber": "SN-002",
					  "hostname": "NB-002",
					  "displayName": "Notebook 002",
					  "description": "Primary laptop",
					  "categoryId": "%s",
					  "manufacturerId": "%s",
					  "currentLocationId": "%s",
					  "currentAssignedUserId": "%s"
					}
					""".formatted(CATEGORY_1, MANUFACTURER_1, LOCATION_1, ASSIGNED_USER_1)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.assetTag").value("ast-002"))
			.andExpect(jsonPath("$.status").value("IN_STOCK"));
	}

	@Test
	void assetManagerUpdatesAssetOfOwnOrganization() throws Exception {
		String token = login("manager1@assetdock.dev", "S3curePass!");

		mockMvc.perform(patch("/assets/{id}", ASSET_1)
				.header(AUTHORIZATION, bearer(token))
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "displayName": "Notebook atualizado",
					  "serialNumber": "SN-UPDATED",
					  "status": "ASSIGNED"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.displayName").value("Notebook atualizado"))
			.andExpect(jsonPath("$.status").value("ASSIGNED"));

		Map<String, Object> event = latestAuditEvent();
		org.assertj.core.api.Assertions.assertThat(event)
			.containsEntry("event_type", "ASSET_UPDATED")
			.containsEntry("outcome", "SUCCESS")
			.containsEntry("organization_id", ORG_1)
			.containsEntry("resource_id", ASSET_1);
	}

	@Test
	void retiredAssetCanBeArchived() throws Exception {
		String token = login("manager1@assetdock.dev", "S3curePass!");

		mockMvc.perform(patch("/assets/{id}/status", ASSET_1)
				.header(AUTHORIZATION, bearer(token))
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "status": "RETIRED"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("RETIRED"));

		mockMvc.perform(patch("/assets/{id}/archive", ASSET_1)
				.header(AUTHORIZATION, bearer(token)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.archivedAt").isNotEmpty());

		Map<String, Object> event = latestAuditEvent();
		org.assertj.core.api.Assertions.assertThat(event)
			.containsEntry("event_type", "ASSET_ARCHIVED")
			.containsEntry("resource_id", ASSET_1);
	}

	@Test
	void activeAssetCannotBeArchived() throws Exception {
		String token = login("manager1@assetdock.dev", "S3curePass!");

		mockMvc.perform(patch("/assets/{id}/archive", ASSET_1)
				.header(AUTHORIZATION, bearer(token)))
			.andExpect(status().isBadRequest());
	}

	@Test
	void archivedAssetCannotBeUpdated() throws Exception {
		archiveAssetDirectly(ASSET_1, ORG_1);
		String token = login("manager1@assetdock.dev", "S3curePass!");

		mockMvc.perform(patch("/assets/{id}", ASSET_1)
				.header(AUTHORIZATION, bearer(token))
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "displayName": "Should fail"
					}
					"""))
			.andExpect(status().isBadRequest());
	}

	@Test
	void auditorCanListAndReadAssetsOfOwnOrganization() throws Exception {
		String token = login("auditor1@assetdock.dev", "S3curePass!");

		mockMvc.perform(get("/assets")
				.header(AUTHORIZATION, bearer(token)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].assetTag").value("AST-001"));

		mockMvc.perform(get("/assets/{id}", ASSET_1)
				.header(AUTHORIZATION, bearer(token)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.assetTag").value("AST-001"));
	}

	@Test
	void viewerCanListAndReadAssetsOfOwnOrganization() throws Exception {
		String token = login("viewer1@assetdock.dev", "S3curePass!");

		mockMvc.perform(get("/assets")
				.header(AUTHORIZATION, bearer(token)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].assetTag").value("AST-001"));

		mockMvc.perform(get("/assets/{id}", ASSET_1)
				.header(AUTHORIZATION, bearer(token)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.assetTag").value("AST-001"));
	}

	@Test
	void crossTenantAccessToAssetsIsDenied() throws Exception {
		String token = login("auditor1@assetdock.dev", "S3curePass!");

		mockMvc.perform(get("/assets/{id}", ASSET_2)
				.header(AUTHORIZATION, bearer(token)))
			.andExpect(status().isNotFound());
	}

	@Test
	void createAndStatusUpdateShouldPersistAssetAuditLogs() throws Exception {
		String token = login("orgadmin1@assetdock.dev", "S3curePass!");

		String response = mockMvc.perform(post("/assets")
				.header(AUTHORIZATION, bearer(token))
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "assetTag": "AST-LOG",
					  "displayName": "Audit asset",
					  "categoryId": "%s",
					  "manufacturerId": "%s",
					  "currentLocationId": "%s"
					}
					""".formatted(CATEGORY_1, MANUFACTURER_1, LOCATION_1)))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();

		int start = response.indexOf("\"id\":\"") + 6;
		int end = response.indexOf('"', start);
		UUID createdAssetId = UUID.fromString(response.substring(start, end));

		Map<String, Object> createdEvent = latestAuditEvent();
		org.assertj.core.api.Assertions.assertThat(createdEvent)
			.containsEntry("event_type", "ASSET_CREATED")
			.containsEntry("outcome", "SUCCESS")
			.containsEntry("organization_id", ORG_1)
			.containsEntry("resource_id", createdAssetId);

		mockMvc.perform(patch("/assets/{id}/status", createdAssetId)
				.header(AUTHORIZATION, bearer(token))
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "status": "LOST"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("LOST"));

		Map<String, Object> updatedEvent = latestAuditEvent();
		org.assertj.core.api.Assertions.assertThat(updatedEvent)
			.containsEntry("event_type", "ASSET_UPDATED")
			.containsEntry("outcome", "SUCCESS")
			.containsEntry("organization_id", ORG_1)
			.containsEntry("resource_id", createdAssetId);
	}

	@Test
	void createShouldFailWhenCategoryBelongsToAnotherTenant() throws Exception {
		String token = login("orgadmin1@assetdock.dev", "S3curePass!");

		mockMvc.perform(post("/assets")
				.header(AUTHORIZATION, bearer(token))
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "assetTag": "AST-XCAT",
					  "displayName": "Cross tenant category",
					  "categoryId": "%s"
					}
					""".formatted(CATEGORY_2)))
			.andExpect(status().isBadRequest());
	}

	@Test
	void createShouldFailWhenManufacturerBelongsToAnotherTenant() throws Exception {
		String token = login("orgadmin1@assetdock.dev", "S3curePass!");

		mockMvc.perform(post("/assets")
				.header(AUTHORIZATION, bearer(token))
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "assetTag": "AST-XMAN",
					  "displayName": "Cross tenant manufacturer",
					  "manufacturerId": "%s"
					}
					""".formatted(MANUFACTURER_2)))
			.andExpect(status().isBadRequest());
	}

	@Test
	void createShouldFailWhenLocationBelongsToAnotherTenant() throws Exception {
		String token = login("orgadmin1@assetdock.dev", "S3curePass!");

		mockMvc.perform(post("/assets")
				.header(AUTHORIZATION, bearer(token))
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "assetTag": "AST-XLOC",
					  "displayName": "Cross tenant location",
					  "currentLocationId": "%s"
					}
					""".formatted(LOCATION_2)))
			.andExpect(status().isBadRequest());
	}

	private Map<String, Object> latestAuditEvent() {
		return jdbcTemplate.queryForMap("""
			SELECT event_type, outcome, organization_id, resource_id
			FROM audit_logs
			ORDER BY occurred_at DESC
			LIMIT 1
			""");
	}

	private void archiveAssetDirectly(UUID assetId, UUID organizationId) {
		jdbcTemplate.update(
			"""
				UPDATE assets
				SET status = 'RETIRED',
				    archived_at = CURRENT_TIMESTAMP,
				    updated_at = CURRENT_TIMESTAMP
				WHERE id = ?
				  AND organization_id = ?
				""",
			assetId,
			organizationId
		);
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
		jdbcTemplate.update(
			"""
				INSERT INTO locations (id, organization_id, name, active, created_at, updated_at)
				VALUES (?, ?, ?, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""",
			id,
			organizationId,
			name
		);
	}

	private void insertAsset(
		UUID id,
		UUID organizationId,
		String assetTag,
		UUID categoryId,
		UUID manufacturerId,
		UUID locationId,
		UUID assignedUserId,
		String status
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
					created_at,
					updated_at
				)
				VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::asset_status, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""",
			id,
			organizationId,
			assetTag,
			assetTag,
			categoryId,
			manufacturerId,
			locationId,
			assignedUserId,
			status
		);
	}

	private void cleanDatabase() {
		jdbcTemplate.update("DELETE FROM audit_logs");
		jdbcTemplate.update("DELETE FROM assets");
		jdbcTemplate.update("DELETE FROM categories");
		jdbcTemplate.update("DELETE FROM manufacturers");
		jdbcTemplate.update("DELETE FROM locations");
		jdbcTemplate.update("DELETE FROM user_roles");
		jdbcTemplate.update("DELETE FROM users");
		jdbcTemplate.update("DELETE FROM organizations");
	}
}
