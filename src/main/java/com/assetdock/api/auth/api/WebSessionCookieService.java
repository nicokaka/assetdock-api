package com.assetdock.api.auth.api;

import com.assetdock.api.auth.application.WebAuthenticatedSession;
import com.assetdock.api.auth.infrastructure.WebSessionProperties;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class WebSessionCookieService {

	private final WebSessionProperties webSessionProperties;

	public WebSessionCookieService(WebSessionProperties webSessionProperties) {
		this.webSessionProperties = webSessionProperties;
	}

	public void writeLoginCookies(HttpServletResponse response, WebAuthenticatedSession authenticatedSession) {
		addCookie(response, ResponseCookie.from(webSessionProperties.sessionCookieName(), authenticatedSession.session().id().toString())
			.httpOnly(true)
			.secure(webSessionProperties.secureCookies())
			.sameSite(webSessionProperties.sameSite())
			.path("/")
			.maxAge(webSessionProperties.absoluteLifetime())
			.build());
		addCookie(response, ResponseCookie.from(webSessionProperties.csrfCookieName(), authenticatedSession.session().csrfToken())
			.httpOnly(false)
			.secure(webSessionProperties.secureCookies())
			.sameSite(webSessionProperties.sameSite())
			.path("/")
			.maxAge(webSessionProperties.absoluteLifetime())
			.build());
	}

	public void clearAuthenticationCookies(HttpServletResponse response) {
		addCookie(response, ResponseCookie.from(webSessionProperties.sessionCookieName(), "")
			.httpOnly(true)
			.secure(webSessionProperties.secureCookies())
			.sameSite(webSessionProperties.sameSite())
			.path("/")
			.maxAge(0)
			.build());
		addCookie(response, ResponseCookie.from(webSessionProperties.csrfCookieName(), "")
			.httpOnly(false)
			.secure(webSessionProperties.secureCookies())
			.sameSite(webSessionProperties.sameSite())
			.path("/")
			.maxAge(0)
			.build());
	}

	public String sessionCookieName() {
		return webSessionProperties.sessionCookieName();
	}

	public String csrfCookieName() {
		return webSessionProperties.csrfCookieName();
	}

	private void addCookie(HttpServletResponse response, ResponseCookie cookie) {
		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
	}

	public String getCookieValue(jakarta.servlet.http.HttpServletRequest request, String cookieName) {
		if (request.getCookies() == null) {
			return null;
		}
		for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
			if (cookieName.equals(cookie.getName())) {
				return cookie.getValue();
			}
		}
		return null;
	}
}
