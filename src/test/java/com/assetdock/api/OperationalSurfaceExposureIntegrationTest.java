package com.assetdock.api;
import com.assetdock.api.support.AbstractIntegrationTest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = {
	"springdoc.api-docs.enabled=false",
	"springdoc.swagger-ui.enabled=false",
	"app.surface.public-docs-enabled=false"
})
class OperationalSurfaceExposureIntegrationTest extends AbstractIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

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
