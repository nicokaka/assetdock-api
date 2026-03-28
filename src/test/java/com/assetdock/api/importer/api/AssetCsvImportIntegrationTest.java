package com.assetdock.api.importer.api;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class AssetCsvImportIntegrationTest {

	private static final UUID ORG_1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID ORG_2 = UUID.fromString("22222222-2222-2222-2222-222222222222");
	private static final UUID ORG_ADMIN_1 = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
	private static final UUID ASSET_MANAGER_1 = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
	private static final UUID AUDITOR_1 = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
	private static final UUID VIEWER_1 = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
	private static final UUID USER_2 = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
	private static final UUID CATEGORY_1 = UUID.fromString("10000000-0000-0000-0000-000000000001");
	private static final UUID MANUFACTURER_1 = UUID.fromString("20000000-0000-0000-0000-000000000002");
	private static final UUID LOCATION_1 = UUID.fromString("30000000-0000-0000-0000-000000000003");
	private static final UUID LOCATION_2 = UUID.fromString("40000000-0000-0000-0000-000000000004");
	private static final UUID IMPORT_JOB_2 = UUID.fromString("50000000-0000-0000-0000-000000000005");

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
		insertUser(USER_2, ORG_2, "user2@assetdock.dev", "VIEWER");
		insertCategory(CATEGORY_1, ORG_1, "Laptops");
		insertManufacturer(MANUFACTURER_1, ORG_1, "Lenovo");
		insertLocation(LOCATION_1, ORG_1, "Warehouse");
		insertLocation(LOCATION_2, ORG_2, "HQ");
		insertImportJob(IMPORT_JOB_2, ORG_2, USER_2, "org2.csv", "COMPLETED");
	}

	@AfterEach
	void tearDown() {
		cleanDatabase();
	}

	@Test
	void orgAdminImportsValidCsvOfOwnOrganization() throws Exception {
		String token = login("orgadmin1@assetdock.dev", "S3curePass!");
		MockMultipartFile file = csvFile("assets.csv", """
			asset_tag,display_name,category_id,manufacturer_id,location_id,status
			AST-CSV-1,Imported laptop,%s,%s,%s,IN_STOCK
			""".formatted(CATEGORY_1, MANUFACTURER_1, LOCATION_1));

		String response = mockMvc.perform(multipart("/imports/assets/csv")
				.file(file)
				.header(AUTHORIZATION, bearer(token))
				.contentType(MULTIPART_FORM_DATA))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("COMPLETED"))
			.andExpect(jsonPath("$.totalRows").value(1))
			.andExpect(jsonPath("$.successCount").value(1))
			.andExpect(jsonPath("$.errorCount").value(0))
			.andReturn()
			.getResponse()
			.getContentAsString();

		UUID jobId = extractId(response);

		mockMvc.perform(get("/imports/assets/{jobId}", jobId)
				.header(AUTHORIZATION, bearer(token)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(jobId.toString()))
			.andExpect(jsonPath("$.status").value("COMPLETED"));
	}

	@Test
	void assetManagerImportsCsvWithPartialSuccess() throws Exception {
		String token = login("manager1@assetdock.dev", "S3curePass!");
		MockMultipartFile file = csvFile("partial.csv", """
			asset_tag,display_name,category_id,manufacturer_id,location_id
			AST-CSV-2,Imported laptop,%s,%s,%s
			AST-CSV-3,,%s,%s,%s
			""".formatted(CATEGORY_1, MANUFACTURER_1, LOCATION_1, CATEGORY_1, MANUFACTURER_1, LOCATION_1));

		mockMvc.perform(multipart("/imports/assets/csv")
				.file(file)
				.header(AUTHORIZATION, bearer(token))
				.contentType(MULTIPART_FORM_DATA))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("COMPLETED_WITH_ERRORS"))
			.andExpect(jsonPath("$.totalRows").value(2))
			.andExpect(jsonPath("$.processedRows").value(2))
			.andExpect(jsonPath("$.successCount").value(1))
			.andExpect(jsonPath("$.errorCount").value(1))
			.andExpect(jsonPath("$.errors[0].line").value(3));
	}

	@Test
	void viewerCannotImportCsv() throws Exception {
		String token = login("viewer1@assetdock.dev", "S3curePass!");
		MockMultipartFile file = csvFile("assets.csv", """
			asset_tag,display_name
			AST-CSV-4,Blocked
			""");

		mockMvc.perform(multipart("/imports/assets/csv")
				.file(file)
				.header(AUTHORIZATION, bearer(token))
				.contentType(MULTIPART_FORM_DATA))
			.andExpect(status().isForbidden());
	}

	@Test
	void crossTenantAccessToImportJobIsDenied() throws Exception {
		String token = login("auditor1@assetdock.dev", "S3curePass!");

		mockMvc.perform(get("/imports/assets/{jobId}", IMPORT_JOB_2)
				.header(AUTHORIZATION, bearer(token)))
			.andExpect(status().isNotFound());
	}

	@Test
	void jobRegistersCountersAndInvalidLineDoesNotAbortImport() throws Exception {
		String token = login("manager1@assetdock.dev", "S3curePass!");
		MockMultipartFile file = csvFile("counters.csv", """
			asset_tag,display_name,location_id
			AST-CSV-5,Imported one,%s
			AST-CSV-6,Imported two,%s
			AST-CSV-7,Invalid location,%s
			""".formatted(LOCATION_1, LOCATION_1, LOCATION_2));

		mockMvc.perform(multipart("/imports/assets/csv")
				.file(file)
				.header(AUTHORIZATION, bearer(token))
				.contentType(MULTIPART_FORM_DATA))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("COMPLETED_WITH_ERRORS"))
			.andExpect(jsonPath("$.totalRows").value(3))
			.andExpect(jsonPath("$.processedRows").value(3))
			.andExpect(jsonPath("$.successCount").value(2))
			.andExpect(jsonPath("$.errorCount").value(1));

		Integer importedAssets = jdbcTemplate.queryForObject("""
			SELECT COUNT(*) FROM assets WHERE organization_id = ?
			""", Integer.class, ORG_1);
		org.assertj.core.api.Assertions.assertThat(importedAssets).isEqualTo(2);
	}

	@Test
	void importGeneratesPersistedAuditLogs() throws Exception {
		String token = login("orgadmin1@assetdock.dev", "S3curePass!");
		MockMultipartFile file = csvFile("audit.csv", """
			asset_tag,display_name
			AST-CSV-8,Audit import
			""");

		mockMvc.perform(multipart("/imports/assets/csv")
				.file(file)
				.header(AUTHORIZATION, bearer(token))
				.contentType(MULTIPART_FORM_DATA))
			.andExpect(status().isCreated());

		Integer startedCount = jdbcTemplate.queryForObject("""
			SELECT COUNT(*) FROM audit_logs WHERE event_type = 'CSV_IMPORT_STARTED'
			""", Integer.class);
		Integer completedCount = jdbcTemplate.queryForObject("""
			SELECT COUNT(*) FROM audit_logs WHERE event_type = 'CSV_IMPORT_COMPLETED'
			""", Integer.class);
		org.assertj.core.api.Assertions.assertThat(startedCount).isEqualTo(1);
		org.assertj.core.api.Assertions.assertThat(completedCount).isEqualTo(1);
	}

	@Test
	void importRejectsEmptyFilePredictably() throws Exception {
		String token = login("orgadmin1@assetdock.dev", "S3curePass!");
		MockMultipartFile file = csvFile("empty.csv", "");

		mockMvc.perform(multipart("/imports/assets/csv")
				.file(file)
				.header(AUTHORIZATION, bearer(token))
				.contentType(MULTIPART_FORM_DATA))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.type").value("urn:assetdock:problem:invalid-asset-import-request"))
			.andExpect(jsonPath("$.detail").value("A non-empty CSV file is required."));

		org.assertj.core.api.Assertions.assertThat(auditEventCount("CSV_IMPORT_FAILED")).isEqualTo(1);
		org.assertj.core.api.Assertions.assertThat(latestAuditOutcome()).isEqualTo("FAILURE");
		org.assertj.core.api.Assertions.assertThat(latestAuditReasonCode()).isEqualTo("empty-file");
	}

	@Test
	void importRejectsMissingRequiredHeaders() throws Exception {
		String token = login("orgadmin1@assetdock.dev", "S3curePass!");
		MockMultipartFile file = csvFile("missing-header.csv", """
			display_name
			Missing asset tag
			""");

		mockMvc.perform(multipart("/imports/assets/csv")
				.file(file)
				.header(AUTHORIZATION, bearer(token))
				.contentType(MULTIPART_FORM_DATA))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("FAILED"))
			.andExpect(jsonPath("$.failureReason").value("CSV must include header 'asset_tag'."));

		org.assertj.core.api.Assertions.assertThat(assetCountForOrg(ORG_1)).isZero();
		org.assertj.core.api.Assertions.assertThat(latestAuditOutcome()).isEqualTo("FAILURE");
		org.assertj.core.api.Assertions.assertThat(latestAuditReasonCode()).isEqualTo("missing-header");
	}

	@Test
	void importRespectsFileSizeLimit() throws Exception {
		String token = login("orgadmin1@assetdock.dev", "S3curePass!");
		String oversizedContent = "asset_tag,display_name\n" + "A".repeat(2 * 1024 * 1024 + 1);
		MockMultipartFile file = csvFile("oversized.csv", oversizedContent);

		mockMvc.perform(multipart("/imports/assets/csv")
				.file(file)
				.header(AUTHORIZATION, bearer(token))
				.contentType(MULTIPART_FORM_DATA))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("FAILED"))
			.andExpect(jsonPath("$.failureReason").value("CSV file size limit exceeded. Maximum is 2 MB."));
	}

	@Test
	void importRespectsLineLimit() throws Exception {
		String token = login("orgadmin1@assetdock.dev", "S3curePass!");
		StringBuilder csv = new StringBuilder("asset_tag,display_name\n");
		for (int index = 1; index <= 1001; index++) {
			csv.append("AST-LINE-").append(index).append(",Line ").append(index).append('\n');
		}
		MockMultipartFile file = csvFile("line-limit.csv", csv.toString());

		mockMvc.perform(multipart("/imports/assets/csv")
				.file(file)
				.header(AUTHORIZATION, bearer(token))
				.contentType(MULTIPART_FORM_DATA))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("FAILED"))
			.andExpect(jsonPath("$.failureReason").value("CSV line limit exceeded. Maximum is 1000 data rows."));
	}

	@Test
	void importRejectsDuplicateAssetTagsWithinSameUploadBeforeWrites() throws Exception {
		String token = login("manager1@assetdock.dev", "S3curePass!");
		MockMultipartFile file = csvFile("duplicate-tags.csv", """
			asset_tag,display_name
			AST-DUP-1,First
			ast-dup-1,Second
			""");

		mockMvc.perform(multipart("/imports/assets/csv")
				.file(file)
				.header(AUTHORIZATION, bearer(token))
				.contentType(MULTIPART_FORM_DATA))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("FAILED"))
			.andExpect(jsonPath("$.failureReason").value("CSV contains duplicated asset_tag values in the same upload."));

		org.assertj.core.api.Assertions.assertThat(assetCountForOrg(ORG_1)).isZero();
		org.assertj.core.api.Assertions.assertThat(latestAuditOutcome()).isEqualTo("FAILURE");
		org.assertj.core.api.Assertions.assertThat(latestAuditReasonCode()).isEqualTo("duplicate-asset-tag-in-upload");
	}

	@Test
	void importTreatsMalformedRowsAsSafePerRowErrors() throws Exception {
		String token = login("manager1@assetdock.dev", "S3curePass!");
		MockMultipartFile file = csvFile("malformed-rows.csv", """
			asset_tag,display_name,category_id
			AST-CSV-9,Valid row,%s
			AST-CSV-10
			""".formatted(CATEGORY_1));

		mockMvc.perform(multipart("/imports/assets/csv")
				.file(file)
				.header(AUTHORIZATION, bearer(token))
				.contentType(MULTIPART_FORM_DATA))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("COMPLETED_WITH_ERRORS"))
			.andExpect(jsonPath("$.totalRows").value(2))
			.andExpect(jsonPath("$.successCount").value(1))
			.andExpect(jsonPath("$.errorCount").value(1))
			.andExpect(jsonPath("$.errors[0].line").value(3))
			.andExpect(jsonPath("$.errors[0].reason").value("Row has invalid CSV structure."));

		org.assertj.core.api.Assertions.assertThat(assetCountForOrg(ORG_1)).isEqualTo(1);
	}

	private MockMultipartFile csvFile(String fileName, String content) {
		return new MockMultipartFile(
			"file",
			fileName,
			"text/csv",
			content.getBytes(StandardCharsets.UTF_8)
		);
	}

	private UUID extractId(String response) {
		int start = response.indexOf("\"id\":\"") + 6;
		int end = response.indexOf('"', start);
		return UUID.fromString(response.substring(start, end));
	}

	private String login(String email, String password) throws Exception {
		String response = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/auth/login")
				.contentType(org.springframework.http.MediaType.APPLICATION_JSON)
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

	private void insertImportJob(UUID id, UUID organizationId, UUID uploadedByUserId, String fileName, String status) {
		jdbcTemplate.update(
			"""
				INSERT INTO asset_import_jobs (
					id,
					organization_id,
					uploaded_by_user_id,
					status,
					file_name,
					total_rows,
					processed_rows,
					success_count,
					error_count,
					result_summary_json,
					started_at,
					finished_at,
					created_at
				)
				VALUES (?, ?, ?, ?::asset_import_job_status, ?, 1, 1, 1, 0, '{}'::jsonb, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""",
			id,
			organizationId,
			uploadedByUserId,
			status,
			fileName
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

	private int assetCountForOrg(UUID organizationId) {
		Integer value = jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM assets WHERE organization_id = ?",
			Integer.class,
			organizationId
		);
		return value == null ? 0 : value;
	}

	private int auditEventCount(String eventType) {
		Integer value = jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM audit_logs WHERE event_type = ?::audit_event_type",
			Integer.class,
			eventType
		);
		return value == null ? 0 : value;
	}

	private String latestAuditOutcome() {
		return jdbcTemplate.queryForObject(
			"SELECT outcome FROM audit_logs ORDER BY occurred_at DESC LIMIT 1",
			String.class
		);
	}

	private String latestAuditReasonCode() {
		return jdbcTemplate.queryForObject(
			"SELECT details_json ->> 'reasonCode' FROM audit_logs ORDER BY occurred_at DESC LIMIT 1",
			String.class
		);
	}
}
