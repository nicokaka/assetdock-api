package com.assetdock.api.catalog.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateManufacturerRequest(
	@NotBlank @Size(max = 150) String name,
	@Size(max = 2000) String description,
	@Size(max = 255) String website,
	Boolean active
) {
}
