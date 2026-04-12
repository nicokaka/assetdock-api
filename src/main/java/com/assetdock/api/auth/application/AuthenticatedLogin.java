package com.assetdock.api.auth.application;

import com.assetdock.api.security.auth.AuthenticatedUserPrincipal;
import com.assetdock.api.user.domain.User;
import java.time.Instant;

public record AuthenticatedLogin(
	User user,
	AuthenticatedUserPrincipal principal,
	Instant authenticatedAt
) {
}
