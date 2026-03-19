package com.assetdock.api.catalog.application;

public class CatalogItemAlreadyExistsException extends RuntimeException {

	public CatalogItemAlreadyExistsException(String itemType, String name) {
		super(itemType + " with name '" + name + "' already exists in this organization.");
	}
}
