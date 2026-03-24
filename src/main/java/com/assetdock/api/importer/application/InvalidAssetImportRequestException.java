package com.assetdock.api.importer.application;

public class InvalidAssetImportRequestException extends RuntimeException {

	public InvalidAssetImportRequestException(String message) {
		super(message);
	}
}
