package com.assetdock.api.catalog.domain;

import java.time.Instant;
import java.util.UUID;

public record Location(
	UUID id,
	UUID organizationId,
	String name,
	String description,
	boolean active,
	Instant createdAt,
	Instant updatedAt
) {
}
