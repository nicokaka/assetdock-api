package com.assetdock.api.security.auth;

import com.assetdock.api.user.domain.User;
import com.assetdock.api.user.domain.UserRole;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class WebSessionAuthenticationToken extends AuthenticatedUserAuthenticationToken {

	public static final String WEB_SESSION_AUTHORITY = "AUTH_WEB_SESSION";

	private final UUID sessionId;
	private final String csrfToken;
	private final User user;

	public WebSessionAuthenticationToken(UUID sessionId, String csrfToken, User user) {
		super(sessionId.toString(), AuthenticatedUserPrincipal.from(user), authorities(user));
		this.sessionId = sessionId;
		this.csrfToken = csrfToken;
		this.user = user;
	}

	public UUID sessionId() {
		return sessionId;
	}

	public String csrfToken() {
		return csrfToken;
	}

	public User user() {
		return user;
	}

	private static Collection<GrantedAuthority> authorities(User user) {
		LinkedHashSet<GrantedAuthority> authorities = user.roles().stream()
			.map(UserRole::authority)
			.map(SimpleGrantedAuthority::new)
			.collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);
		authorities.add(new SimpleGrantedAuthority(WEB_SESSION_AUTHORITY));
		return authorities;
	}
}
