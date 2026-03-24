package com.assetdock.api.assignment.application;

import java.util.UUID;

public record AssignAssetCommand(
	UUID userId,
	UUID locationId,
	String notes
) {
}
