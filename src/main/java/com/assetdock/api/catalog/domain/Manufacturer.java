package com.assetdock.api.catalog.domain;

import java.time.Instant;
import java.util.UUID;

public record Manufacturer(
	UUID id,
	UUID organizationId,
	String name,
	String description,
	String website,
	boolean active,
	Instant createdAt,
	Instant updatedAt
) {
}
