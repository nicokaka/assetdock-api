package com.assetdock.api.catalog.application;

import java.time.Instant;
import java.util.UUID;

public record CategoryView(
	UUID id,
	String name,
	String description,
	boolean active,
	Instant createdAt,
	Instant updatedAt
) {
}
