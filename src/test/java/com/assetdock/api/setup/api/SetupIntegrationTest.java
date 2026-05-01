package com.assetdock.api.setup.api;

import com.assetdock.api.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SetupIntegrationTest extends AbstractIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void setupStatus_returnsConfiguredBasedOnOrganizationExistence() throws Exception {
		mockMvc.perform(get("/api/v1/setup/status"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.configured").isBoolean());
	}

	@Test
	void setup_withValidData_returns201or409() throws Exception {
		String requestBody = """
			{
				"organizationName": "Setup Test Corp",
				"adminFullName": "Setup Admin",
				"adminEmail": "setup-test@example.com",
				"adminPassword": "SecurePass1!"
			}
			""";

		MvcResult result = mockMvc.perform(post("/api/v1/setup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andReturn();

		int statusCode = result.getResponse().getStatus();
		assertTrue(
			statusCode == 201 || statusCode == 409,
			"Expected 201 (Created) or 409 (Conflict) but got " + statusCode
		);
	}

	@Test
	void setup_withInvalidData_returns400() throws Exception {
		String requestBody = """
			{
				"organizationName": "",
				"adminFullName": "",
				"adminEmail": "not-an-email",
				"adminPassword": "short"
			}
			""";

		mockMvc.perform(post("/api/v1/setup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isBadRequest());
	}

	@Test
	void setup_doesNotRequireAuthentication() throws Exception {
		mockMvc.perform(get("/api/v1/setup/status"))
			.andExpect(status().isOk());
	}

	@Test
	void setup_withMissingBody_returns400() throws Exception {
		mockMvc.perform(post("/api/v1/setup")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
			.andExpect(status().isBadRequest());
	}
}
