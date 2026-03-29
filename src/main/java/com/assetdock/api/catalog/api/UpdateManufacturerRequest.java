package com.assetdock.api.catalog.api;

import jakarta.validation.constraints.Size;

public record UpdateManufacturerRequest(
	@Size(max = 150) String name,
	String description,
	@Size(max = 255) String website,
	Boolean active
) {
}
