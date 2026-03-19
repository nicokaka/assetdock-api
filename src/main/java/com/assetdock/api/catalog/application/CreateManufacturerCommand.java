package com.assetdock.api.catalog.application;

public record CreateManufacturerCommand(
	String name,
	String description,
	String website,
	boolean active
) {
}
