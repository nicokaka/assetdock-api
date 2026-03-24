package com.assetdock.api.asset.api;

import com.assetdock.api.asset.domain.AssetStatus;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.util.UUID;

public record CreateAssetRequest(
	@NotBlank(message = "assetTag is required")
	String assetTag,
	String serialNumber,
	String hostname,
	@NotBlank(message = "displayName is required")
	String displayName,
	String description,
	UUID categoryId,
	UUID manufacturerId,
	UUID currentLocationId,
	UUID currentAssignedUserId,
	AssetStatus status,
	LocalDate purchaseDate,
	LocalDate warrantyExpiryDate
) {
}
