package com.assetdock.api.security.auth;

import com.assetdock.api.auth.api.WebSessionCookieService;
import com.assetdock.api.auth.application.WebAuthenticatedSession;
import com.assetdock.api.auth.application.WebSessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class WebSessionAuthenticationFilter extends OncePerRequestFilter {

	private final WebSessionService webSessionService;
	private final WebSessionCookieService webSessionCookieService;

	public WebSessionAuthenticationFilter(
		WebSessionService webSessionService,
		WebSessionCookieService webSessionCookieService
	) {
		this.webSessionService = webSessionService;
		this.webSessionCookieService = webSessionCookieService;
	}

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		if (SecurityContextHolder.getContext().getAuthentication() != null || hasAuthorizationHeader(request)) {
			filterChain.doFilter(request, response);
			return;
		}

		String rawSessionId = cookieValue(request, webSessionCookieService.sessionCookieName());
		Optional<WebAuthenticatedSession> authenticatedSession = webSessionService.resolve(rawSessionId);
		if (authenticatedSession.isEmpty()) {
			filterChain.doFilter(request, response);
			return;
		}

		WebAuthenticatedSession session = authenticatedSession.get();
		SecurityContextHolder.getContext().setAuthentication(
			new WebSessionAuthenticationToken(session.session().id(), session.session().csrfToken(), session.user())
		);
		filterChain.doFilter(request, response);
	}

	private boolean hasAuthorizationHeader(HttpServletRequest request) {
		String authorization = request.getHeader("Authorization");
		return authorization != null && !authorization.isBlank();
	}

	private String cookieValue(HttpServletRequest request, String cookieName) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null || cookies.length == 0) {
			return null;
		}

		return Arrays.stream(cookies)
			.filter(cookie -> cookieName.equals(cookie.getName()))
			.map(Cookie::getValue)
			.findFirst()
			.orElse(null);
	}
}
