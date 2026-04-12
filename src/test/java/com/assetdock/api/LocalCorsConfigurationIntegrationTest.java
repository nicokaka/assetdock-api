package com.assetdock.api;

import com.assetdock.api.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS;
import static org.springframework.http.HttpHeaders.ORIGIN;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("local")
class LocalCorsConfigurationIntegrationTest extends AbstractIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@DynamicPropertySource
	static void configureLocalProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRES::getUsername);
		registry.add("spring.datasource.password", POSTGRES::getPassword);
		registry.add("security.jwt.secret", () -> "test-only-jwt-secret-key-with-32-bytes");
		registry.add("app.seed.enabled", () -> "false");
	}

	@Test
	void configuredLocalFrontendOriginIsAllowed() throws Exception {
		mockMvc.perform(options("/assets")
				.header(ORIGIN, "http://localhost:5173")
				.header(ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type, X-CSRF-Token")
				.header(ACCESS_CONTROL_REQUEST_METHOD, "GET"))
			.andExpect(status().isOk())
			.andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
			.andExpect(header().string("Access-Control-Allow-Credentials", "true"))
			.andExpect(header().string("Access-Control-Allow-Headers", containsString("X-CSRF-Token")))
			.andExpect(header().string("Vary", containsString("Origin")));
	}

	@Test
	void unknownOriginIsRejectedInLocalProfile() throws Exception {
		mockMvc.perform(options("/assets")
				.header(ORIGIN, "http://evil.example")
				.header(ACCESS_CONTROL_REQUEST_METHOD, "GET"))
			.andExpect(status().isForbidden());
	}
}
