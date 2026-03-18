package com.assetdock.api.auth.application;

import com.assetdock.api.security.auth.AuthenticatedUserPrincipal;

public record LoginResult(
	String accessToken,
	long expiresInSeconds,
	AuthenticatedUserPrincipal principal
) {
}
