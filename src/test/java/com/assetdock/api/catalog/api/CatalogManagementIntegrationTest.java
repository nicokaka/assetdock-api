package com.assetdock.api.catalog.api;

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
class CatalogManagementIntegrationTest {

	private static final UUID ORG_1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID ORG_2 = UUID.fromString("22222222-2222-2222-2222-222222222222");
	private static final UUID ORG_ADMIN_1 = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
	private static final UUID ASSET_MANAGER_1 = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
	private static final UUID AUDITOR_1 = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
	private static final UUID VIEWER_1 = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
	private static final UUID CATEGORY_1 = UUID.fromString("11111111-2222-3333-4444-555555555555");
	private static final UUID CATEGORY_2 = UUID.fromString("66666666-7777-8888-9999-000000000000");
	private static final UUID MANUFACTURER_1 = UUID.fromString("12345678-1234-1234-1234-123456789012");
	private static final UUID LOCATION_1 = UUID.fromString("21098765-4321-4321-4321-210987654321");

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
		insertCategory(CATEGORY_1, ORG_1, "Laptops");
		insertCategory(CATEGORY_2, ORG_2, "Servers");
		insertManufacturer(MANUFACTURER_1, ORG_1, "Lenovo");
		insertLocation(LOCATION_1, ORG_1, "Warehouse");
	}

	@AfterEach
	void tearDown() {
		cleanDatabase();
	}

	@Test
	void orgAdminCreatesCategoryOfOwnOrganization() throws Exception {
		String token = login("orgadmin1@assetdock.dev", "S3curePass!");

		mockMvc.perform(post("/categories")
				.header(AUTHORIZATION, bearer(token))
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "name": "Monitors",
					  "description": "Display devices",
					  "active": true
					}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.name").value("Monitors"))
			.andExpect(jsonPath("$.active").value(true));
	}

	@Test
	void assetManagerCreatesManufacturerOfOwnOrganization() throws Exception {
		String token = login("manager1@assetdock.dev", "S3curePass!");

		mockMvc.perform(post("/manufacturers")
				.header(AUTHORIZATION, bearer(token))
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "name": "Dell",
					  "description": "Hardware vendor",
					  "website": "https://dell.example",
					  "active": true
					}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.name").value("Dell"));
	}

	@Test
	void orgAdminUpdatesCategoryAndCanDeactivateIt() throws Exception {
		String token = login("orgadmin1@assetdock.dev", "S3curePass!");

		mockMvc.perform(patch("/categories/{id}", CATEGORY_1)
				.header(AUTHORIZATION, bearer(token))
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "name": "Workstations",
					  "active": false
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Workstations"))
			.andExpect(jsonPath("$.active").value(false));

		Map<String, Object> event = latestAuditEvent();
		org.assertj.core.api.Assertions.assertThat(event)
			.containsEntry("event_type", "CATEGORY_UPDATED")
			.containsEntry("resource_id", CATEGORY_1);
	}

	@Test
	void assetManagerUpdatesManufacturerAndCanDeactivateIt() throws Exception {
		String token = login("manager1@assetdock.dev", "S3curePass!");

		mockMvc.perform(patch("/manufacturers/{id}", MANUFACTURER_1)
				.header(AUTHORIZATION, bearer(token))
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "website": "https://lenovo.example",
					  "active": false
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.website").value("https://lenovo.example"))
			.andExpect(jsonPath("$.active").value(false));

		Map<String, Object> event = latestAuditEvent();
		org.assertj.core.api.Assertions.assertThat(event)
			.containsEntry("event_type", "MANUFACTURER_UPDATED")
			.containsEntry("resource_id", MANUFACTURER_1);
	}

	@Test
	void assetManagerUpdatesLocationAndCanDeactivateIt() throws Exception {
		String token = login("manager1@assetdock.dev", "S3curePass!");

		mockMvc.perform(patch("/locations/{id}", LOCATION_1)
				.header(AUTHORIZATION, bearer(token))
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "description": "Secondary storage",
					  "active": false
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.description").value("Secondary storage"))
			.andExpect(jsonPath("$.active").value(false));

		Map<String, Object> event = latestAuditEvent();
		org.assertj.core.api.Assertions.assertThat(event)
			.containsEntry("event_type", "LOCATION_UPDATED")
			.containsEntry("resource_id", LOCATION_1);
	}

	@Test
	void auditorListsCatalogsOfOwnOrganization() throws Exception {
		String token = login("auditor1@assetdock.dev", "S3curePass!");

		mockMvc.perform(get("/categories")
				.header(AUTHORIZATION, bearer(token)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].name").value("Laptops"));
	}

	@Test
	void viewerListsCatalogsOfOwnOrganization() throws Exception {
		String token = login("viewer1@assetdock.dev", "S3curePass!");

		mockMvc.perform(get("/locations")
				.header(AUTHORIZATION, bearer(token)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].name").value("Warehouse"));
	}

	@Test
	void viewerAndAuditorCannotMutateCatalogs() throws Exception {
		String viewerToken = login("viewer1@assetdock.dev", "S3curePass!");
		String auditorToken = login("auditor1@assetdock.dev", "S3curePass!");

		mockMvc.perform(post("/categories")
				.header(AUTHORIZATION, bearer(viewerToken))
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "name": "Blocked Viewer Category"
					}
					"""))
			.andExpect(status().isForbidden());

		mockMvc.perform(patch("/manufacturers/{id}", MANUFACTURER_1)
				.header(AUTHORIZATION, bearer(auditorToken))
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "active": false
					}
					"""))
			.andExpect(status().isForbidden());
	}

	@Test
	void crossTenantCatalogsAreIsolated() throws Exception {
		String token = login("auditor1@assetdock.dev", "S3curePass!");

		mockMvc.perform(get("/categories")
				.header(AUTHORIZATION, bearer(token)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].name").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("Servers"))));
	}

	@Test
	void catalogCreationPersistsAuditLog() throws Exception {
		String token = login("orgadmin1@assetdock.dev", "S3curePass!");

		mockMvc.perform(post("/categories")
				.header(AUTHORIZATION, bearer(token))
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "name": "Docking Stations",
					  "description": "Laptop docks",
					  "active": true
					}
					"""))
			.andExpect(status().isCreated());

		Map<String, Object> event = jdbcTemplate.queryForMap("""
			SELECT event_type, outcome, organization_id
			FROM audit_logs
			ORDER BY occurred_at DESC
			LIMIT 1
			""");
		org.assertj.core.api.Assertions.assertThat(event)
			.containsEntry("event_type", "CATEGORY_CREATED")
			.containsEntry("outcome", "SUCCESS")
			.containsEntry("organization_id", ORG_1);
	}

	private Map<String, Object> latestAuditEvent() {
		return jdbcTemplate.queryForMap("""
			SELECT event_type, outcome, organization_id, resource_id
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

	private void insertCategory(UUID categoryId, UUID organizationId, String name) {
		jdbcTemplate.update(
			"""
				INSERT INTO categories (id, organization_id, name, active, created_at, updated_at)
				VALUES (?, ?, ?, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""",
			categoryId,
			organizationId,
			name
		);
	}

	private void insertManufacturer(UUID manufacturerId, UUID organizationId, String name) {
		jdbcTemplate.update(
			"""
				INSERT INTO manufacturers (id, organization_id, name, active, created_at, updated_at)
				VALUES (?, ?, ?, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""",
			manufacturerId,
			organizationId,
			name
		);
	}

	private void insertLocation(UUID locationId, UUID organizationId, String name) {
		jdbcTemplate.update(
			"""
				INSERT INTO locations (id, organization_id, name, active, created_at, updated_at)
				VALUES (?, ?, ?, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""",
			locationId,
			organizationId,
			name
		);
	}

	private void cleanDatabase() {
		jdbcTemplate.update("DELETE FROM audit_logs");
		jdbcTemplate.update("DELETE FROM categories");
		jdbcTemplate.update("DELETE FROM manufacturers");
		jdbcTemplate.update("DELETE FROM locations");
		jdbcTemplate.update("DELETE FROM user_roles");
		jdbcTemplate.update("DELETE FROM users");
		jdbcTemplate.update("DELETE FROM organizations");
	}
}
