package com.assetdock.api.person.domain;

import java.time.Instant;
import java.util.UUID;

public record Person(
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
