package com.assetdock.api.catalog.application;

public class InvalidCatalogRequestException extends RuntimeException {

	public InvalidCatalogRequestException(String message) {
		super(message);
	}
}
