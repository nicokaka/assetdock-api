package com.assetdock.api.assignment.api;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AssignAssetRequest(
	@NotNull(message = "userId is required")
	UUID userId,
	UUID locationId,
	String notes
) {
}
