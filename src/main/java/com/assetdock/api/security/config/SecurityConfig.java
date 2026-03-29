package com.assetdock.api.security.config;

import com.assetdock.api.security.auth.JwtToAuthenticatedUserConverter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
public class SecurityConfig {

	private final boolean publicDocsEnabled;
	private final String allowedOrigins;

	public SecurityConfig(
		@Value("${app.surface.public-docs-enabled:false}") boolean publicDocsEnabled,
		@Value("${app.surface.allowed-origins:}") String allowedOrigins
	) {
		this.publicDocsEnabled = publicDocsEnabled;
		this.allowedOrigins = allowedOrigins;
	}

	@Bean
	SecurityFilterChain securityFilterChain(
		HttpSecurity http,
		SecurityProblemSupport securityProblemSupport,
		JwtToAuthenticatedUserConverter jwtToAuthenticatedUserConverter
	) throws Exception {
		http
			.csrf(AbstractHttpConfigurer::disable)
			.cors(withDefaults())
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.logout(AbstractHttpConfigurer::disable)
			.requestCache(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.headers(headers -> headers
				.contentTypeOptions(withDefaults())
				.frameOptions(frame -> frame.deny())
				.referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.NO_REFERRER))
				.cacheControl(withDefaults()))
			.exceptionHandling(exceptions -> exceptions
				.authenticationEntryPoint(securityProblemSupport)
				.accessDeniedHandler(securityProblemSupport))
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers(publicEndpoints()).permitAll()
				.anyRequest().authenticated())
			.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtToAuthenticatedUserConverter)));

		return http.build();
	}

	private String[] publicEndpoints() {
		List<String> endpoints = new ArrayList<>();
		endpoints.add("/actuator/health");
		endpoints.add("/api/v1/auth/login");
		if (publicDocsEnabled) {
			endpoints.add("/swagger-ui.html");
			endpoints.add("/swagger-ui/**");
			endpoints.add("/v3/api-docs/**");
		}

		return endpoints.toArray(String[]::new);
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource() {
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		List<String> origins = Arrays.stream(allowedOrigins.split(","))
			.map(String::trim)
			.filter(origin -> !origin.isBlank())
			.toList();
		if (origins.isEmpty()) {
			return source;
		}

		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(origins);
		configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
		configuration.setAllowCredentials(false);
		configuration.setMaxAge(3600L);
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
}
