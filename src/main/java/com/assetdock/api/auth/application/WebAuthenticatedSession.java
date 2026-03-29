package com.assetdock.api.auth.application;

import com.assetdock.api.auth.domain.WebSession;
import com.assetdock.api.security.auth.AuthenticatedUserPrincipal;
import com.assetdock.api.user.domain.User;

public record WebAuthenticatedSession(
	WebSession session,
	User user,
	AuthenticatedUserPrincipal principal
) {
}
