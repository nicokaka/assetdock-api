package com.assetdock.api.importer.domain;

import java.util.List;

public record AssetImportSummary(
	List<AssetImportError> errors,
	String failureReason
) {
}
