package com.assetdock.api.security.auth;

import com.assetdock.api.user.domain.User;
import com.assetdock.api.user.domain.UserRole;
import java.util.Set;
import java.util.UUID;

public record AuthenticatedUserPrincipal(
	UUID userId,
	UUID organizationId,
	String email,
	Set<UserRole> roles
) {

	public static AuthenticatedUserPrincipal from(User user) {
		return new AuthenticatedUserPrincipal(
			user.id(),
			user.organizationId(),
			user.email(),
			Set.copyOf(user.roles())
		);
	}

	public boolean hasRole(UserRole role) {
		return roles.contains(role);
	}

	public boolean isSuperAdmin() {
		return hasRole(UserRole.SUPER_ADMIN);
	}
}
