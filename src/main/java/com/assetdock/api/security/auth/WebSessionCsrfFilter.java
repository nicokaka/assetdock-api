package com.assetdock.api.security.auth;

import com.assetdock.api.auth.api.WebSessionCookieService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.assetdock.api.security.config.SecurityProblemSupport;

@Component
public class WebSessionCsrfFilter extends OncePerRequestFilter {

	private static final String LOGIN_PATH = "/auth/login";
	private static final String WEB_LOGIN_PATH = "/web/auth/login";

	private final WebSessionCookieService webSessionCookieService;
	private final SecurityProblemSupport securityProblemSupport;

	public WebSessionCsrfFilter(
		WebSessionCookieService webSessionCookieService,
		SecurityProblemSupport securityProblemSupport
	) {
		this.webSessionCookieService = webSessionCookieService;
		this.securityProblemSupport = securityProblemSupport;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !requiresCsrfProtection(request) || isLoginRequest(request);
	}

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (!(authentication instanceof WebSessionAuthenticationToken webAuthentication)) {
			filterChain.doFilter(request, response);
			return;
		}

		String csrfCookie = webSessionCookieService.getCookieValue(request, webSessionCookieService.csrfCookieName());
		String csrfHeader = request.getHeader("X-CSRF-Token");
		if (!constantTimeEquals(csrfCookie, csrfHeader)
			|| !constantTimeEquals(csrfHeader, webAuthentication.csrfToken())) {
			securityProblemSupport.handle(request, response, new org.springframework.security.access.AccessDeniedException("Invalid CSRF token."));
			return;
		}

		filterChain.doFilter(request, response);
	}

	private boolean requiresCsrfProtection(HttpServletRequest request) {
		String method = request.getMethod();
		return "POST".equalsIgnoreCase(method)
			|| "PUT".equalsIgnoreCase(method)
			|| "PATCH".equalsIgnoreCase(method)
			|| "DELETE".equalsIgnoreCase(method);
	}

	private boolean isLoginRequest(HttpServletRequest request) {
		String path = getLookupPath(request);
		return "POST".equalsIgnoreCase(request.getMethod())
			&& (WEB_LOGIN_PATH.equals(path) || LOGIN_PATH.equals(path));
	}

	private String getLookupPath(HttpServletRequest request) {
		String servletPath = request.getServletPath();
		if (servletPath != null && !servletPath.isEmpty()) {
			return servletPath;
		}
		String requestUri = request.getRequestURI();
		String contextPath = request.getContextPath();
		if (contextPath != null && requestUri.startsWith(contextPath)) {
			return requestUri.substring(contextPath.length());
		}
		return requestUri;
	}

	private boolean constantTimeEquals(String first, String second) {
		if (first == null || second == null) {
			return false;
		}
		return MessageDigest.isEqual(
			first.getBytes(StandardCharsets.UTF_8),
			second.getBytes(StandardCharsets.UTF_8)
		);
	}
}
