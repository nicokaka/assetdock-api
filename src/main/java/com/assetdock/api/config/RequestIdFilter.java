package com.assetdock.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

	public static final String REQUEST_ID_HEADER = "X-Request-Id";
	public static final String REQUEST_ID_ATTRIBUTE = RequestIdFilter.class.getName() + ".requestId";

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		String requestId = request.getHeader(REQUEST_ID_HEADER);
		if (requestId == null || requestId.isBlank()) {
			requestId = UUID.randomUUID().toString();
		} else {
			requestId = sanitize(requestId);
		}

		request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
		response.setHeader(REQUEST_ID_HEADER, requestId);

		filterChain.doFilter(request, response);
	}

	private String sanitize(String value) {
		String sanitized = value.replaceAll("[^a-zA-Z0-9-]", "");
		if (sanitized.isBlank()) {
			return UUID.randomUUID().toString();
		}

		return sanitized.length() > 64 ? sanitized.substring(0, 64) : sanitized;
	}
}
