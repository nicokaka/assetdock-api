package com.assetdock.api.catalog.application;

public record UpdateManufacturerCommand(String name, String description, String website, Boolean active) {
}
