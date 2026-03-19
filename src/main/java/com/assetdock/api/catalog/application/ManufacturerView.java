package com.assetdock.api.catalog.application;

import java.time.Instant;
import java.util.UUID;

public record ManufacturerView(
	UUID id,
	String name,
	String description,
	String website,
	boolean active,
	Instant createdAt,
	Instant updatedAt
) {
}
