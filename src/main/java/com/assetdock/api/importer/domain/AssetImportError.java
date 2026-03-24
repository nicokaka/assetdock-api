package com.assetdock.api.importer.domain;

public record AssetImportError(
	int line,
	String reason
) {
}
