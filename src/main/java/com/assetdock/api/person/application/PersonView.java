package com.assetdock.api.person.application;

import java.time.Instant;
import java.util.UUID;

public record PersonView(
	UUID id,
	UUID organizationId,
	String fullName,
	String email,
	String department,
	boolean active,
	Instant createdAt,
	Instant updatedAt
) {
}
