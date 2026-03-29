package com.assetdock.api.catalog.application;

public record UpdateCategoryCommand(String name, String description, Boolean active) {
}
