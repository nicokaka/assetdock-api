package com.assetdock.api.assignment.application;

import java.time.Instant;
import java.util.UUID;

public record AssetAssignmentView(
	UUID id,
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
