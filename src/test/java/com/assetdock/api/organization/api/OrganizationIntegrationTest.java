package com.assetdock.api.organization.api;
import com.assetdock.api.support.AbstractIntegrationTest;

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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrganizationIntegrationTest extends AbstractIntegrationTest {

	private static final UUID ORG_1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID ORG_2 = UUID.fromString("22222222-2222-2222-2222-222222222222");
	private static final UUID ORG_ADMIN_1 = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
	private static final UUID SUPER_ADMIN = UUID.fromString("99999999-9999-9999-9999-999999999999");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private JwtTokenService jwtTokenService;

	@BeforeEach
	void setUp() {
		cleanDatabase();
		jdbcTemplate.update(
			"INSERT INTO organizations (id, name, slug) VALUES (?, ?, ?)",
			ORG_1,
			"Org One",
			"org-one"
		);
		jdbcTemplate.update(
			"INSERT INTO organizations (id, name, slug) VALUES (?, ?, ?)",
			ORG_2,
			"Org Two",
			"org-two"
		);
	}

	@AfterEach
	void tearDown() {
		cleanDatabase();
	}

	@Test
	void ownOrganizationReadIsAllowed() throws Exception {
		mockMvc.perform(get("/organizations/{id}", ORG_1)
				.header(AUTHORIZATION, bearer(tokenForOrgAdmin())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(ORG_1.toString()))
			.andExpect(jsonPath("$.slug").value("org-one"));
	}

	@Test
	void crossTenantOrganizationReadIsDenied() throws Exception {
		mockMvc.perform(get("/organizations/{id}", ORG_2)
				.header(AUTHORIZATION, bearer(tokenForOrgAdmin())))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.type").value("urn:assetdock:problem:access-denied"));
	}

	@Test
	void superAdminCanReadAnyOrganization() throws Exception {
		mockMvc.perform(get("/organizations/{id}", ORG_2)
				.header(AUTHORIZATION, bearer(tokenForSuperAdmin())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(ORG_2.toString()))
			.andExpect(jsonPath("$.slug").value("org-two"));
	}

	private String tokenForOrgAdmin() {
		return jwtTokenService.issue(
			new AuthenticatedUserPrincipal(
				ORG_ADMIN_1,
				ORG_1,
				"orgadmin1@assetdock.dev",
				Set.of(UserRole.ORG_ADMIN)
			),
			Instant.now()
		).value();
	}

	private String tokenForSuperAdmin() {
		return jwtTokenService.issue(
			new AuthenticatedUserPrincipal(
				SUPER_ADMIN,
				null,
				"superadmin@assetdock.dev",
				Set.of(UserRole.SUPER_ADMIN)
			),
			Instant.now()
		).value();
	}

	private String bearer(String token) {
		return "Bearer " + token;
	}

	private void cleanDatabase() {
		jdbcTemplate.update("DELETE FROM organizations");
	}
}
