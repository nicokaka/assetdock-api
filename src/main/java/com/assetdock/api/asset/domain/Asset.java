package com.assetdock.api.asset.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record Asset(
	UUID id,
	UUID organizationId,
	String assetTag,
	String serialNumber,
	String hostname,
	String displayName,
	String description,
	UUID categoryId,
	UUID manufacturerId,
	UUID currentLocationId,
	UUID currentAssignedUserId,
	AssetStatus status,
	LocalDate purchaseDate,
	LocalDate warrantyExpiryDate,
	Instant archivedAt,
	Instant createdAt,
	Instant updatedAt
) {
}
