package com.assetdock.api.asset.application;

public class AssetNotFoundException extends RuntimeException {

	public AssetNotFoundException() {
		super("Asset not found.");
	}
}
