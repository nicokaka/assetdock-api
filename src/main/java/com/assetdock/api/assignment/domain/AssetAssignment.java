package com.assetdock.api.assignment.domain;

import java.time.Instant;
import java.util.UUID;

public record AssetAssignment(
	UUID id,
	UUID organizationId,
	UUID assetId,
	UUID userId,
	UUID locationId,
	Instant assignedAt,
	Instant unassignedAt,
	UUID assignedBy,
	String notes,
	Instant createdAt
) {
}
