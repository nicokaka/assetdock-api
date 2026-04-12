package com.assetdock.api.auth.api;

import java.util.UUID;

public record WebSessionResponse(AuthenticatedUserResponse user) {

	public record AuthenticatedUserResponse(
		UUID id,
		String fullName,
		String email,
		String role,
		UUID organizationId
	) {
	}
}
