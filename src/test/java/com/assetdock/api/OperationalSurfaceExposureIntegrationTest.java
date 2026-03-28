package com.assetdock.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
	"springdoc.api-docs.enabled=false",
	"springdoc.swagger-ui.enabled=false",
	"app.surface.public-docs-enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class OperationalSurfaceExposureIntegrationTest {

	@Container
	static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
		.withDatabaseName("assetdock_test")
		.withUsername("assetdock")
		.withPassword("assetdock");

	@Autowired
	private MockMvc mockMvc;

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRES::getUsername);
		registry.add("spring.datasource.password", POSTGRES::getPassword);
		registry.add("security.jwt.secret", () -> "test-only-jwt-secret-key-with-32-bytes");
	}

	@Test
	void openApiDocsAreNotPublicWhenDisabled() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.type").value("urn:assetdock:problem:authentication-required"));
	}

	@Test
	void swaggerUiIsNotPublicWhenDisabled() throws Exception {
		mockMvc.perform(get("/swagger-ui.html"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.type").value("urn:assetdock:problem:authentication-required"));
	}
}
