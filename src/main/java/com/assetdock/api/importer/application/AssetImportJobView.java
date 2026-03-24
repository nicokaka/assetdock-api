package com.assetdock.api.importer.application;

import com.assetdock.api.importer.domain.AssetImportError;
import com.assetdock.api.importer.domain.AssetImportJobStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AssetImportJobView(
	UUID id,
	AssetImportJobStatus status,
	String fileName,
	int totalRows,
	int processedRows,
	int successCount,
	int errorCount,
	List<AssetImportError> errors,
	String failureReason,
	Instant startedAt,
	Instant finishedAt,
	Instant createdAt
) {
}
