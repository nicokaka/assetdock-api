package com.assetdock.api.asset.application;

public class InvalidAssetRequestException extends RuntimeException {

	public InvalidAssetRequestException(String message) {
		super(message);
	}
}
