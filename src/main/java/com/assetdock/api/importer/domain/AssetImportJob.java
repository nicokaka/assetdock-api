package com.assetdock.api.importer.domain;

import java.time.Instant;
import java.util.UUID;

public record AssetImportJob(
	UUID id,
	UUID organizationId,
	UUID uploadedByUserId,
	AssetImportJobStatus status,
	String fileName,
	int totalRows,
	int processedRows,
	int successCount,
	int errorCount,
	AssetImportSummary summary,
	Instant startedAt,
	Instant finishedAt,
	Instant createdAt
) {
}
