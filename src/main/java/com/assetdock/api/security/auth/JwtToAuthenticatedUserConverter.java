package com.assetdock.api.security.auth;

import com.assetdock.api.user.domain.UserRole;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class JwtToAuthenticatedUserConverter implements Converter<Jwt, AuthenticatedUserAuthenticationToken> {

	@Override
	public AuthenticatedUserAuthenticationToken convert(Jwt jwt) {
		Set<UserRole> roles = jwt.getClaimAsStringList("roles") == null
			? Set.of()
			: jwt.getClaimAsStringList("roles")
				.stream()
				.map(UserRole::valueOf)
				.collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);

		Collection<GrantedAuthority> authorities = roles.stream()
			.map(UserRole::authority)
			.map(authority -> (GrantedAuthority) new SimpleGrantedAuthority(authority))
			.toList();

		String organizationId = jwt.getClaimAsString("organization_id");
		AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(
			UUID.fromString(jwt.getClaimAsString("user_id")),
			organizationId == null ? null : UUID.fromString(organizationId),
			jwt.getClaimAsString("email"),
			roles
		);

		return new AuthenticatedUserAuthenticationToken(jwt.getTokenValue(), principal, authorities);
	}
}
