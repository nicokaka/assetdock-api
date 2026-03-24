package com.assetdock.api.asset.api;

import com.assetdock.api.asset.domain.AssetStatus;
import java.time.LocalDate;
import java.util.UUID;

public record UpdateAssetRequest(
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
	LocalDate warrantyExpiryDate
) {
}
