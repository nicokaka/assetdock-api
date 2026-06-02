package com.assetdock.api.assignment.application;

import java.util.UUID;

public record AssignAssetCommand(
	UUID personId,
	UUID locationId,
	String notes
) {
}
