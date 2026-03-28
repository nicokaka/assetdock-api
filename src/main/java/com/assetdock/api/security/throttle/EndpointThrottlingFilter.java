package com.assetdock.api.security.throttle;

import com.assetdock.api.common.error.ProblemDetailFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class EndpointThrottlingFilter extends OncePerRequestFilter {

	private static final String LOGIN_PATH = "/api/v1/auth/login";
	private static final String IMPORT_PATH = "/imports/assets/csv";

	private final EndpointRateLimiter endpointRateLimiter;
	private final ThrottlingProperties throttlingProperties;
	private final ProblemDetailFactory problemDetailFactory;
	private final ObjectMapper objectMapper;
	private final Clock clock;

	public EndpointThrottlingFilter(
		EndpointRateLimiter endpointRateLimiter,
		ThrottlingProperties throttlingProperties,
		ProblemDetailFactory problemDetailFactory,
		ObjectMapper objectMapper,
		Clock clock
	) {
		this.endpointRateLimiter = endpointRateLimiter;
		this.throttlingProperties = throttlingProperties;
		this.problemDetailFactory = problemDetailFactory;
		this.objectMapper = objectMapper;
		this.clock = clock;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !isLoginRequest(request) && !isImportRequest(request);
	}

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		EndpointRateLimiter.Endpoint endpoint = resolveEndpoint(request);
		ThrottlingProperties.EndpointPolicy policy = endpoint == EndpointRateLimiter.Endpoint.LOGIN
			? throttlingProperties.login()
			: throttlingProperties.assetImport();

		EndpointRateLimiter.RateLimitDecision decision = endpointRateLimiter.tryAcquire(
			endpoint,
			resolveOrigin(request),
			policy,
			Instant.now(clock)
		);
		if (decision.allowed()) {
			filterChain.doFilter(request, response);
			return;
		}

		ProblemDetail problemDetail = problemDetailFactory.create(
			HttpStatus.TOO_MANY_REQUESTS,
			"Too many requests",
			"Too many requests. Please retry later.",
			"urn:assetdock:problem:rate-limit-exceeded",
			request.getRequestURI(),
			Map.of("retryAfterSeconds", decision.retryAfterSeconds())
		);
		response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
		response.setHeader("Retry-After", String.valueOf(decision.retryAfterSeconds()));
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		objectMapper.writeValue(response.getOutputStream(), problemDetail);
	}

	private boolean isLoginRequest(HttpServletRequest request) {
		return "POST".equalsIgnoreCase(request.getMethod()) && LOGIN_PATH.equals(request.getRequestURI());
	}

	private boolean isImportRequest(HttpServletRequest request) {
		return "POST".equalsIgnoreCase(request.getMethod()) && IMPORT_PATH.equals(request.getRequestURI());
	}

	private EndpointRateLimiter.Endpoint resolveEndpoint(HttpServletRequest request) {
		return isLoginRequest(request)
			? EndpointRateLimiter.Endpoint.LOGIN
			: EndpointRateLimiter.Endpoint.ASSET_IMPORT;
	}

	private String resolveOrigin(HttpServletRequest request) {
		String forwardedFor = request.getHeader("X-Forwarded-For");
		if (forwardedFor != null && !forwardedFor.isBlank()) {
			return forwardedFor.split(",")[0].trim();
		}

		String remoteAddress = request.getRemoteAddr();
		return remoteAddress == null || remoteAddress.isBlank() ? "unknown" : remoteAddress;
	}
}
