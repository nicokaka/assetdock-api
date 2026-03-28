package com.assetdock.api.importer.api;

import com.assetdock.api.support.TestJwtTokens;
import com.assetdock.api.user.domain.UserRole;
import java.nio.charset.StandardCharsets;
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

import static com.assetdock.api.support.MockMvcClientIp.uniqueClientIp;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
	"security.throttle.enabled=true",
	"security.throttle.login.enabled=false",
	"security.throttle.login.max-requests=100",
	"security.throttle.asset-import.enabled=true",
	"security.throttle.asset-import.max-requests=1",
	"security.throttle.asset-import.window=PT10M"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class AssetCsvImportThrottlingIntegrationTest {

	private static final UUID ORGANIZATION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID USER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
	private static final UUID CATEGORY_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
	private static final UUID MANUFACTURER_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");
	private static final UUID LOCATION_ID = UUID.fromString("30000000-0000-0000-0000-000000000003");
	private static final String CLIENT_IP = "203.0.113.20";

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
		insertOrganization();
		insertUser();
		insertCategory();
		insertManufacturer();
		insertLocation();
	}

	@AfterEach
	void tearDown() {
		cleanDatabase();
	}

	@Test
	void shouldThrottleRepeatedImportAttemptsFromSameClient() throws Exception {
		String token = login("orgadmin1@assetdock.dev", "S3curePass!");
		MockMultipartFile firstFile = csvFile("first.csv", """
			asset_tag,display_name,category_id,manufacturer_id,location_id,status
			AST-CSV-THROTTLE-1,Imported laptop,%s,%s,%s,IN_STOCK
			""".formatted(CATEGORY_ID, MANUFACTURER_ID, LOCATION_ID));
		MockMultipartFile secondFile = csvFile("second.csv", """
			asset_tag,display_name
			AST-CSV-THROTTLE-2,Imported laptop
			""");

		mockMvc.perform(multipart("/imports/assets/csv")
				.file(firstFile)
				.with(request -> {
					request.setRemoteAddr(CLIENT_IP);
					return request;
				})
				.header(AUTHORIZATION, bearer(token))
				.contentType(MULTIPART_FORM_DATA))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("COMPLETED"));

		mockMvc.perform(multipart("/imports/assets/csv")
				.file(secondFile)
				.with(request -> {
					request.setRemoteAddr(CLIENT_IP);
					return request;
				})
				.header(AUTHORIZATION, bearer(token))
				.contentType(MULTIPART_FORM_DATA))
			.andExpect(status().isTooManyRequests())
			.andExpect(header().exists("Retry-After"))
			.andExpect(jsonPath("$.type").value("urn:assetdock:problem:rate-limit-exceeded"))
			.andExpect(jsonPath("$.detail").value("Too many requests. Please retry later."));

		org.assertj.core.api.Assertions.assertThat(importJobCount()).isEqualTo(1);
	}

	private String login(String email, String password) {
		return TestJwtTokens.issue(USER_ID, ORGANIZATION_ID, email, java.util.Set.of(UserRole.ORG_ADMIN));
	}

	private MockMultipartFile csvFile(String fileName, String content) {
		return new MockMultipartFile(
			"file",
			fileName,
			"text/csv",
			content.getBytes(StandardCharsets.UTF_8)
		);
	}

	private String bearer(String token) {
		return "Bearer " + token;
	}

	private void insertOrganization() {
		jdbcTemplate.update(
			"INSERT INTO organizations (id, name, slug) VALUES (?, ?, ?)",
			ORGANIZATION_ID,
			"org-one",
			"org-one"
		);
	}

	private void insertUser() {
		jdbcTemplate.update(
			"""
				INSERT INTO users (id, organization_id, email, full_name, password_hash, status)
				VALUES (?, ?, ?, ?, ?, 'ACTIVE'::user_status)
				""",
			USER_ID,
			ORGANIZATION_ID,
			"orgadmin1@assetdock.dev",
			"Org Admin",
			passwordEncoder.encode("S3curePass!")
		);
		jdbcTemplate.update(
			"""
				INSERT INTO user_roles (user_id, role)
				VALUES (?, 'ORG_ADMIN'::user_role)
				""",
			USER_ID
		);
	}

	private void insertCategory() {
		jdbcTemplate.update(
			"""
				INSERT INTO categories (id, organization_id, name, active, created_at, updated_at)
				VALUES (?, ?, ?, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""",
			CATEGORY_ID,
			ORGANIZATION_ID,
			"Laptops"
		);
	}

	private void insertManufacturer() {
		jdbcTemplate.update(
			"""
				INSERT INTO manufacturers (id, organization_id, name, active, created_at, updated_at)
				VALUES (?, ?, ?, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""",
			MANUFACTURER_ID,
			ORGANIZATION_ID,
			"Lenovo"
		);
	}

	private void insertLocation() {
		jdbcTemplate.update(
			"""
				INSERT INTO locations (id, organization_id, name, active, created_at, updated_at)
				VALUES (?, ?, ?, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""",
			LOCATION_ID,
			ORGANIZATION_ID,
			"Warehouse"
		);
	}

	private int importJobCount() {
		Integer value = jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM asset_import_jobs WHERE organization_id = ?",
			Integer.class,
			ORGANIZATION_ID
		);
		return value == null ? 0 : value;
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
