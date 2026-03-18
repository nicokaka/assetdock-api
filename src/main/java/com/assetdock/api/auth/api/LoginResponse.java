package com.assetdock.api.auth.api;

import java.util.Set;
import java.util.UUID;

public record LoginResponse(
	String accessToken,
	String tokenType,
	long expiresInSeconds,
	AuthenticatedUserResponse user
) {

	public record AuthenticatedUserResponse(
		UUID userId,
		UUID organizationId,
		String email,
		Set<String> roles
	) {
	}
}
