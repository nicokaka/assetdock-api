package com.assetdock.api.catalog.application;

public record CreateLocationCommand(String name, String description, boolean active) {
}
