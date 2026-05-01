package com.assetdock.api.setup.api;

import java.util.UUID;

public record SetupResponse(
	OrganizationSummary organization,
	AdminSummary admin
) {

	public record OrganizationSummary(UUID id, String name) {
	}

	public record AdminSummary(UUID id, String email, String fullName) {
	}
}
