package com.assetdock.api.security.auth;

import java.util.Collection;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

public class AuthenticatedUserAuthenticationToken extends AbstractAuthenticationToken {

	private final String tokenValue;
	private final AuthenticatedUserPrincipal principal;

	public AuthenticatedUserAuthenticationToken(
		String tokenValue,
		AuthenticatedUserPrincipal principal,
		Collection<? extends GrantedAuthority> authorities
	) {
		super(authorities);
		this.tokenValue = tokenValue;
		this.principal = principal;
		setAuthenticated(true);
	}

	@Override
	public Object getCredentials() {
		return tokenValue;
	}

	@Override
	public Object getPrincipal() {
		return principal;
	}

	@Override
	public String getName() {
		return principal.email();
	}
}
