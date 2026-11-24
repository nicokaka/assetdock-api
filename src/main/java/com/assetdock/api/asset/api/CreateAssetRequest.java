package com.assetdock.api.asset.api;

import com.assetdock.api.asset.domain.AssetStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record CreateAssetRequest(
	@NotBlank(message = "assetTag is required")
	@Size(max = 120, message = "assetTag must not exceed 120 characters")
	String assetTag,
	@Size(max = 150, message = "serialNumber must not exceed 150 characters")
	String serialNumber,
	@Size(max = 150, message = "hostname must not exceed 150 characters")
	String hostname,
	@NotBlank(message = "displayName is required")
	@Size(max = 200, message = "displayName must not exceed 200 characters")
	String displayName,
	@Size(max = 2000, message = "description must not exceed 2000 characters")
	String description,
	UUID categoryId,
	UUID manufacturerId,
	UUID currentLocationId,
	UUID currentAssignedPersonId,
	AssetStatus status,
	LocalDate purchaseDate,
	LocalDate warrantyExpiryDate
) {
}
