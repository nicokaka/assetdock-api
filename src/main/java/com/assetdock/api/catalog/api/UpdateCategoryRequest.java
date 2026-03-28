package com.assetdock.api.catalog.api;

import jakarta.validation.constraints.Size;

public record UpdateCategoryRequest(
	@Size(max = 150) String name,
	String description,
	Boolean active
) {
}
