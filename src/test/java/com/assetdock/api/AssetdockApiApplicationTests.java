package com.assetdock.api;

import com.assetdock.api.config.LocalDevelopmentSeedRunner;
import com.assetdock.api.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AssetdockApiApplicationTests extends AbstractIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired(required = false)
	private LocalDevelopmentSeedRunner localDevelopmentSeedRunner;

	@Test
	void contextLoads() {
	}


	@Test
	void healthEndpointIsPublic() throws Exception {
		mockMvc.perform(get("/actuator/health"))
			.andExpect(status().isOk());
	}

	@Test
	void actuatorInfoRequiresAuthentication() throws Exception {
		mockMvc.perform(get("/actuator/info"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void openApiDocsAreAvailableInTestProfile() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk());
	}

	@Test
	void protectedEndpointsRequireAuthentication() throws Exception {
		mockMvc.perform(get("/api/internal"))
			.andExpect(status().isUnauthorized());
	}
}
