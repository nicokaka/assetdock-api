package com.assetdock.api.organization.application;

import java.time.Instant;
import java.util.UUID;

public record OrganizationView(
	UUID id,
	String name,
	String slug,
	Instant createdAt,
	Instant updatedAt
) {
}
