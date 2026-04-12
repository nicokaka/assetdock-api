package com.assetdock.api.security.auth;

import com.assetdock.api.auth.api.WebSessionCookieService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.assetdock.api.security.config.SecurityProblemSupport;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class WebSessionCsrfFilter extends OncePerRequestFilter {

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
		return !requiresCsrfProtection(request);
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
		if (csrfCookie == null
			|| csrfHeader == null
			|| !csrfCookie.equals(csrfHeader)
			|| !csrfHeader.equals(webAuthentication.csrfToken())) {
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


}
