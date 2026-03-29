package com.assetdock.api;

import com.assetdock.api.auth.infrastructure.JwtTokenService;
import com.assetdock.api.security.auth.AuthenticatedUserPrincipal;
import com.assetdock.api.user.domain.UserRole;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class ApiContractErrorIntegrationTest {

	private static final UUID ORGANIZATION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID ACTOR_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
	private static final UUID ASSET_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

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
	private JwtTokenService jwtTokenService;

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
		jdbcTemplate.update(
			"INSERT INTO organizations (id, name, slug) VALUES (?, ?, ?)",
			ORGANIZATION_ID,
			"AssetDock",
			"assetdock"
		);
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
	void invalidUuidPathReturnsBadRequest() throws Exception {
		mockMvc.perform(get("/organizations/not-a-uuid")
				.header(AUTHORIZATION, bearer(token())))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.type").value("urn:assetdock:problem:malformed-request"));
	}

	@Test
	void invalidEnumValueReturnsBadRequest() throws Exception {
		mockMvc.perform(patch("/assets/{id}/status", ASSET_ID)
				.header(AUTHORIZATION, bearer(token()))
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "status": "NOT_A_REAL_STATUS"
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.type").value("urn:assetdock:problem:malformed-request"));
	}

	@Test
	void malformedJsonReturnsBadRequest() throws Exception {
		mockMvc.perform(post("/users")
				.header(AUTHORIZATION, bearer(token()))
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "fullName": "Broken JSON",
					  "email": "broken@assetdock.dev",
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.type").value("urn:assetdock:problem:malformed-request"));
	}

	@Test
	void multipartWithoutFileReturnsBadRequest() throws Exception {
		mockMvc.perform(post("/imports/assets/csv")
				.header(AUTHORIZATION, bearer(token()))
				.contentType(MULTIPART_FORM_DATA))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.type").value("urn:assetdock:problem:request-failed"));
	}

	@Test
	void missingRequiredFieldsReturnValidationError() throws Exception {
		mockMvc.perform(post("/users")
				.header(AUTHORIZATION, bearer(token()))
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "fullName": "User Without Required Fields",
					  "password": "S3curePass!"
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.type").value("urn:assetdock:problem:validation-error"));
	}

	private String token() {
		return jwtTokenService.issue(
			new AuthenticatedUserPrincipal(
				ACTOR_ID,
				ORGANIZATION_ID,
				"orgadmin@assetdock.dev",
				Set.of(UserRole.ORG_ADMIN)
			),
			Instant.now()
		).value();
	}

	private String bearer(String token) {
		return "Bearer " + token;
	}

	private void cleanDatabase() {
		jdbcTemplate.update("DELETE FROM assets");
		jdbcTemplate.update("DELETE FROM organizations");
	}
}
