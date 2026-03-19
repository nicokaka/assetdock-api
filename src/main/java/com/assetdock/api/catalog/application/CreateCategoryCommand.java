package com.assetdock.api.catalog.application;

public record CreateCategoryCommand(String name, String description, boolean active) {
}
