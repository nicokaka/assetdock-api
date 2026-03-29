package com.assetdock.api.importer.application;

import com.assetdock.api.asset.application.AssetAlreadyExistsException;
import com.assetdock.api.asset.application.AssetManagementService;
import com.assetdock.api.asset.application.CreateAssetCommand;
import com.assetdock.api.asset.application.InvalidAssetRequestException;
import com.assetdock.api.asset.domain.AssetStatus;
import com.assetdock.api.audit.application.AuditLogCommand;
import com.assetdock.api.audit.application.AuditLogService;
import com.assetdock.api.audit.domain.AuditEventType;
import com.assetdock.api.importer.domain.AssetImportError;
import com.assetdock.api.importer.domain.AssetImportJob;
import com.assetdock.api.importer.domain.AssetImportJobRepository;
import com.assetdock.api.importer.domain.AssetImportJobStatus;
import com.assetdock.api.importer.domain.AssetImportSummary;
import com.assetdock.api.security.auth.AuthenticatedUserPrincipal;
import com.assetdock.api.security.auth.TenantAccessService;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AssetCsvImportService {

	private static final long MAX_FILE_SIZE_BYTES = 2L * 1024L * 1024L;
	private static final int MAX_DATA_ROWS = 1000;
	private static final List<String> REQUIRED_COLUMNS = List.of("asset_tag", "display_name");
	private static final String INVALID_ROW_STRUCTURE_REASON = "Row has invalid CSV structure.";
	private static final String INVALID_CSV_STRUCTURE_MESSAGE = "Invalid CSV structure.";

	private final AssetImportJobRepository assetImportJobRepository;
	private final AssetManagementService assetManagementService;
	private final TenantAccessService tenantAccessService;
	private final AuditLogService auditLogService;
	private final Clock clock;

	public AssetCsvImportService(
		AssetImportJobRepository assetImportJobRepository,
		AssetManagementService assetManagementService,
		TenantAccessService tenantAccessService,
		AuditLogService auditLogService,
		Clock clock
	) {
		this.assetImportJobRepository = assetImportJobRepository;
		this.assetManagementService = assetManagementService;
		this.tenantAccessService = tenantAccessService;
		this.auditLogService = auditLogService;
		this.clock = clock;
	}

	public AssetImportJobView importCsv(AuthenticatedUserPrincipal actor, MultipartFile file) {
		UUID organizationId = requireActorOrganizationId(actor);
		tenantAccessService.requireImportWriteAccess(actor, organizationId);

		if (file == null || file.isEmpty()) {
			recordImportFailureAttempt(organizationId, actor.userId(), sanitizeFileName(file == null ? null : file.getOriginalFilename()), "empty-file");
			throw new InvalidAssetImportRequestException("empty-file", "A non-empty CSV file is required.");
		}

		Instant now = Instant.now(clock);
		AssetImportJob job = new AssetImportJob(
			UUID.randomUUID(),
			organizationId,
			actor.userId(),
			AssetImportJobStatus.PROCESSING,
			sanitizeFileName(file.getOriginalFilename()),
			0,
			0,
			0,
			0,
			new AssetImportSummary(List.of(), null),
			now,
			null,
			now
		);
		assetImportJobRepository.save(job);
		recordAudit(job, actor.userId(), AuditEventType.CSV_IMPORT_STARTED, Map.of("fileName", job.fileName()), "SUCCESS");

		try {
			ParsedCsv parsedCsv = parseRecords(file);
			validateDuplicateAssetTags(parsedCsv.records(), parsedCsv.headerCount());
			List<AssetImportError> errors = new ArrayList<>();
			int successCount = 0;

			for (CSVRecord record : parsedCsv.records()) {
				int lineNumber = Math.toIntExact(record.getRecordNumber()) + 1;
				if (isMalformedRow(record, parsedCsv.headerCount())) {
					errors.add(new AssetImportError(lineNumber, INVALID_ROW_STRUCTURE_REASON));
					continue;
				}
				try {
					assetManagementService.create(actor, new CreateAssetCommand(
						requiredValue(record, "asset_tag"),
						optionalValue(record, "serial_number"),
						optionalValue(record, "hostname"),
						requiredValue(record, "display_name"),
						optionalValue(record, "description"),
						optionalUuid(record, "category_id"),
						optionalUuid(record, "manufacturer_id"),
						optionalUuid(record, "location_id"),
						null,
						optionalStatus(record, "status"),
						optionalDate(record, "purchase_date"),
						optionalDate(record, "warranty_expiry_date")
					));
					successCount++;
				}
				catch (Exception exception) {
					errors.add(new AssetImportError(lineNumber, toRowReason(exception)));
				}
			}

			AssetImportJob completedJob = new AssetImportJob(
				job.id(),
				job.organizationId(),
				job.uploadedByUserId(),
				errors.isEmpty() ? AssetImportJobStatus.COMPLETED : AssetImportJobStatus.COMPLETED_WITH_ERRORS,
				job.fileName(),
				parsedCsv.records().size(),
				parsedCsv.records().size(),
				successCount,
				errors.size(),
				new AssetImportSummary(List.copyOf(errors), null),
				job.startedAt(),
				Instant.now(clock),
				job.createdAt()
			);
			assetImportJobRepository.update(completedJob);
			recordAudit(completedJob, actor.userId(), AuditEventType.CSV_IMPORT_COMPLETED, Map.of(
				"totalRows", completedJob.totalRows(),
				"successCount", completedJob.successCount(),
				"errorCount", completedJob.errorCount()
			), "SUCCESS");
			return toView(completedJob);
		}
		catch (InvalidAssetImportRequestException exception) {
			AssetImportJob failedJob = failJob(job, exception.getMessage());
			recordAudit(failedJob, actor.userId(), AuditEventType.CSV_IMPORT_FAILED, Map.of(
				"reasonCode", exception.reasonCode(),
				"fileName", failedJob.fileName()
			), "FAILURE");
			return toView(failedJob);
		}
		catch (IOException exception) {
			AssetImportJob failedJob = failJob(job, "The CSV file could not be read.");
			recordAudit(failedJob, actor.userId(), AuditEventType.CSV_IMPORT_FAILED, Map.of(
				"reasonCode", "file-read-error",
				"fileName", failedJob.fileName()
			), "FAILURE");
			return toView(failedJob);
		}
	}

	public AssetImportJobView getJob(AuthenticatedUserPrincipal actor, UUID jobId) {
		AssetImportJob job;
		if (actor.isSuperAdmin()) {
			job = assetImportJobRepository.findById(jobId).orElseThrow(AssetImportJobNotFoundException::new);
		}
		else {
			UUID organizationId = requireActorOrganizationId(actor);
			tenantAccessService.requireImportReadAccess(actor, organizationId);
			job = assetImportJobRepository.findByIdAndOrganizationId(jobId, organizationId)
				.orElseThrow(AssetImportJobNotFoundException::new);
		}

		return toView(job);
	}

	private ParsedCsv parseRecords(MultipartFile file) throws IOException {
		if (file.getSize() > MAX_FILE_SIZE_BYTES) {
			throw new InvalidAssetImportRequestException("file-size-limit-exceeded", "CSV file size limit exceeded. Maximum is 2 MB.");
		}

		try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
		     CSVParser parser = CSVFormat.DEFAULT.builder()
				.setHeader()
				.setSkipHeaderRecord(true)
				.setIgnoreEmptyLines(true)
				.setTrim(true)
				.build()
				.parse(reader)) {
			List<String> headers = parser.getHeaderNames();
			if (headers == null || headers.isEmpty()) {
				throw new InvalidAssetImportRequestException("missing-header", "CSV header is required.");
			}
			validateHeaders(headers);
			List<CSVRecord> records = parser.getRecords();
			if (records.isEmpty()) {
				throw new InvalidAssetImportRequestException("empty-data-rows", "CSV must include at least one data row.");
			}
			if (records.size() > MAX_DATA_ROWS) {
				throw new InvalidAssetImportRequestException("line-limit-exceeded", "CSV line limit exceeded. Maximum is 1000 data rows.");
			}
			return new ParsedCsv(records, headers.size());
		}
		catch (InvalidAssetImportRequestException exception) {
			throw exception;
		}
		catch (RuntimeException exception) {
			throw new InvalidAssetImportRequestException("invalid-structure", INVALID_CSV_STRUCTURE_MESSAGE);
		}
	}

	private void validateHeaders(List<String> headers) {
		Set<String> exactHeaders = new HashSet<>();
		Set<String> duplicateGuard = new HashSet<>();
		for (String header : headers) {
			if (header == null || header.isBlank()) {
				throw new InvalidAssetImportRequestException("invalid-header", INVALID_CSV_STRUCTURE_MESSAGE);
			}

			String trimmedHeader = header.trim();
			if (!duplicateGuard.add(trimmedHeader.toLowerCase(java.util.Locale.ROOT))) {
				throw new InvalidAssetImportRequestException("invalid-header", INVALID_CSV_STRUCTURE_MESSAGE);
			}
			exactHeaders.add(trimmedHeader);
		}

		for (String requiredColumn : REQUIRED_COLUMNS) {
			if (!exactHeaders.contains(requiredColumn)) {
				throw new InvalidAssetImportRequestException("missing-header", "CSV must include header '" + requiredColumn + "'.");
			}
		}
	}

	private void validateDuplicateAssetTags(List<CSVRecord> records, int expectedColumnCount) {
		Set<String> seenAssetTags = new HashSet<>();
		for (CSVRecord record : records) {
			if (isMalformedRow(record, expectedColumnCount)) {
				continue;
			}

			String assetTag = safeOptionalValue(record, "asset_tag");
			if (assetTag == null) {
				continue;
			}

			String normalizedAssetTag = assetTag.trim().toLowerCase(java.util.Locale.ROOT);
			if (!seenAssetTags.add(normalizedAssetTag)) {
				throw new InvalidAssetImportRequestException(
					"duplicate-asset-tag-in-upload",
					"CSV contains duplicated asset_tag values in the same upload."
				);
			}
		}
	}

	private boolean isMalformedRow(CSVRecord record, int expectedColumnCount) {
		return record == null || record.size() != expectedColumnCount;
	}

	private AssetImportJob failJob(AssetImportJob job, String failureReason) {
		AssetImportJob failedJob = new AssetImportJob(
			job.id(),
			job.organizationId(),
			job.uploadedByUserId(),
			AssetImportJobStatus.FAILED,
			job.fileName(),
			job.totalRows(),
			job.processedRows(),
			job.successCount(),
			job.errorCount(),
			new AssetImportSummary(List.of(), failureReason),
			job.startedAt(),
			Instant.now(clock),
			job.createdAt()
		);
		return assetImportJobRepository.update(failedJob);
	}

	private void recordAudit(
		AssetImportJob job,
		UUID actorUserId,
		AuditEventType eventType,
		Map<String, Object> details,
		String outcome
	) {
		auditLogService.record(new AuditLogCommand(
			job.organizationId(),
			actorUserId,
			eventType,
			"asset_import_job",
			job.id(),
			outcome,
			details
		));
	}

	private void recordImportFailureAttempt(UUID organizationId, UUID actorUserId, String fileName, String reasonCode) {
		auditLogService.record(new AuditLogCommand(
			organizationId,
			actorUserId,
			AuditEventType.CSV_IMPORT_FAILED,
			"asset_import_job",
			null,
			"FAILURE",
			Map.of(
				"reasonCode", reasonCode,
				"fileName", fileName
			)
		));
	}

	private AssetImportJobView toView(AssetImportJob job) {
		return new AssetImportJobView(
			job.id(),
			job.status(),
			job.fileName(),
			job.totalRows(),
			job.processedRows(),
			job.successCount(),
			job.errorCount(),
			job.summary() == null ? List.of() : job.summary().errors(),
			job.summary() == null ? null : job.summary().failureReason(),
			job.startedAt(),
			job.finishedAt(),
			job.createdAt()
		);
	}

	private UUID requireActorOrganizationId(AuthenticatedUserPrincipal actor) {
		if (actor.organizationId() == null) {
			throw new InvalidAssetImportRequestException("Tenant organization context is required for CSV imports.");
		}

		return actor.organizationId();
	}

	private String sanitizeFileName(String originalFileName) {
		if (originalFileName == null || originalFileName.isBlank()) {
			return "upload.csv";
		}

		String normalized = originalFileName.replace('\\', '/');
		String fileName = normalized.substring(normalized.lastIndexOf('/') + 1).trim();
		return fileName.isEmpty() ? "upload.csv" : fileName.substring(0, Math.min(fileName.length(), 255));
	}

	private String requiredValue(CSVRecord record, String column) {
		String value = optionalValue(record, column);
		if (value == null) {
			throw new InvalidAssetImportRequestException(column + " is required.");
		}
		return value;
	}

	private String optionalValue(CSVRecord record, String column) {
		if (!record.isMapped(column)) {
			return null;
		}
		String value = record.get(column);
		return value == null || value.isBlank() ? null : value.trim();
	}

	private String safeOptionalValue(CSVRecord record, String column) {
		try {
			return optionalValue(record, column);
		}
		catch (RuntimeException exception) {
			return null;
		}
	}

	private UUID optionalUuid(CSVRecord record, String column) {
		String value = optionalValue(record, column);
		if (value == null) {
			return null;
		}
		try {
			return UUID.fromString(value);
		}
		catch (IllegalArgumentException exception) {
			throw new InvalidAssetImportRequestException(column + " must be a valid UUID.");
		}
	}

	private LocalDate optionalDate(CSVRecord record, String column) {
		String value = optionalValue(record, column);
		if (value == null) {
			return null;
		}
		try {
			return LocalDate.parse(value);
		}
		catch (DateTimeParseException exception) {
			throw new InvalidAssetImportRequestException(column + " must use ISO-8601 date format (yyyy-MM-dd).");
		}
	}

	private AssetStatus optionalStatus(CSVRecord record, String column) {
		String value = optionalValue(record, column);
		if (value == null) {
			return null;
		}
		try {
			return AssetStatus.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
		}
		catch (IllegalArgumentException exception) {
			throw new InvalidAssetImportRequestException(column + " must be a valid asset status.");
		}
	}

	private String toRowReason(Exception exception) {
		if (exception instanceof InvalidAssetImportRequestException
			|| exception instanceof InvalidAssetRequestException
			|| exception instanceof AssetAlreadyExistsException) {
			return exception.getMessage();
		}

		return "Row could not be processed.";
	}

	private record ParsedCsv(
		List<CSVRecord> records,
		int headerCount
	) {
	}
}
