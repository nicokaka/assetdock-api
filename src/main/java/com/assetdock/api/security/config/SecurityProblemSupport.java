package com.assetdock.api.security.config;

import com.assetdock.api.common.error.ProblemDetailFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class SecurityProblemSupport implements AuthenticationEntryPoint, AccessDeniedHandler {

	private final ObjectMapper objectMapper;
	private final ProblemDetailFactory problemDetailFactory;

	public SecurityProblemSupport(ObjectMapper objectMapper, ProblemDetailFactory problemDetailFactory) {
		this.objectMapper = objectMapper;
		this.problemDetailFactory = problemDetailFactory;
	}

	@Override
	public void commence(
		HttpServletRequest request,
		HttpServletResponse response,
		org.springframework.security.core.AuthenticationException authException
	) throws IOException {
		writeProblem(
			response,
			problemDetailFactory.create(
				HttpStatus.UNAUTHORIZED,
				"Authentication required",
				"Authentication is required to access this resource.",
				"urn:assetdock:problem:authentication-required",
				request.getRequestURI()
			)
		);
	}

	@Override
	public void handle(
		HttpServletRequest request,
		HttpServletResponse response,
		AccessDeniedException accessDeniedException
	) throws IOException, ServletException {
		writeProblem(
			response,
			problemDetailFactory.create(
				HttpStatus.FORBIDDEN,
				"Access denied",
				"You do not have permission to access this resource.",
				"urn:assetdock:problem:access-denied",
				request.getRequestURI()
			)
		);
	}

	private void writeProblem(HttpServletResponse response, ProblemDetail problemDetail) throws IOException {
		response.setStatus(problemDetail.getStatus());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		objectMapper.writeValue(response.getOutputStream(), problemDetail);
	}
}
