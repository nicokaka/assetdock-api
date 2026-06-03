package com.assetdock.api.assignment.api;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AssignAssetRequest(
	@NotNull(message = "personId is required")
	UUID personId,
	UUID locationId,
	String notes
) {
}
