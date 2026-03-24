package com.assetdock.api.asset.application;

public class AssetAlreadyExistsException extends RuntimeException {

	public AssetAlreadyExistsException(String assetTag) {
		super("Asset with tag '%s' already exists in this organization.".formatted(assetTag));
	}
}
