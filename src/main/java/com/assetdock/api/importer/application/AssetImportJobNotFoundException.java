package com.assetdock.api.importer.application;

public class AssetImportJobNotFoundException extends RuntimeException {

	public AssetImportJobNotFoundException() {
		super("Asset import job not found.");
	}
}
