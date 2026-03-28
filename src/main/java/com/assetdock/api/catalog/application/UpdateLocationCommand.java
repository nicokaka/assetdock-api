package com.assetdock.api.catalog.application;

public record UpdateLocationCommand(String name, String description, Boolean active) {
}
