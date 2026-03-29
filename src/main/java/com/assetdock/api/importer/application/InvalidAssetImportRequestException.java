package com.assetdock.api.importer.application;

public class InvalidAssetImportRequestException extends RuntimeException {

	private final String reasonCode;

	public InvalidAssetImportRequestException(String message) {
		this("invalid-request", message);
	}

	public InvalidAssetImportRequestException(String reasonCode, String message) {
		super(message);
		this.reasonCode = reasonCode;
	}

	public String reasonCode() {
		return reasonCode;
	}
}
