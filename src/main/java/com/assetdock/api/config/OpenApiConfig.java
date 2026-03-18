package com.assetdock.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	private final String applicationVersion;

	public OpenApiConfig(@Value("${info.app.version}") String applicationVersion) {
		this.applicationVersion = applicationVersion;
	}

	@Bean
	OpenAPI assetdockOpenApi() {
		SecurityScheme bearerScheme = new SecurityScheme()
			.type(SecurityScheme.Type.HTTP)
			.scheme("bearer")
			.bearerFormat("JWT");

		return new OpenAPI()
			.info(new Info()
				.title("AssetDock API")
				.version(applicationVersion)
				.description("Multi-tenant asset inventory API scaffold with security-first defaults."))
			.addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
			.components(new Components().addSecuritySchemes("bearerAuth", bearerScheme));
	}
}
