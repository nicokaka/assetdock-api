package com.assetdock.api.importer.infrastructure;

import com.assetdock.api.importer.domain.AssetImportJob;
import com.assetdock.api.importer.domain.AssetImportJobRepository;
import com.assetdock.api.importer.domain.AssetImportJobStatus;
import com.assetdock.api.importer.domain.AssetImportSummary;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAssetImportJobRepository implements AssetImportJobRepository {

	private final JdbcClient jdbcClient;
	private final ObjectMapper objectMapper;

	public JdbcAssetImportJobRepository(JdbcClient jdbcClient, ObjectMapper objectMapper) {
		this.jdbcClient = jdbcClient;
		this.objectMapper = objectMapper;
	}

	@Override
	public AssetImportJob save(AssetImportJob job) {
		jdbcClient.sql("""
			INSERT INTO asset_import_jobs (
				id,
				organization_id,
				uploaded_by_user_id,
				status,
				file_name,
				total_rows,
				processed_rows,
				success_count,
				error_count,
				result_summary_json,
				started_at,
				finished_at,
				created_at
			)
			VALUES (
				:id,
				:organizationId,
				:uploadedByUserId,
				:status,
				:fileName,
				:totalRows,
				:processedRows,
				:successCount,
				:errorCount,
				CAST(:summaryJson AS jsonb),
				:startedAt,
				:finishedAt,
				:createdAt
			)
			""")
			.param("id", job.id())
			.param("organizationId", job.organizationId())
			.param("uploadedByUserId", job.uploadedByUserId())
			.param("status", job.status().name())
			.param("fileName", job.fileName())
			.param("totalRows", job.totalRows())
			.param("processedRows", job.processedRows())
			.param("successCount", job.successCount())
			.param("errorCount", job.errorCount())
			.param("summaryJson", serializeSummary(job.summary()))
			.param("startedAt", job.startedAt())
			.param("finishedAt", job.finishedAt())
			.param("createdAt", job.createdAt())
			.update();

		return job;
	}

	@Override
	public AssetImportJob update(AssetImportJob job) {
		jdbcClient.sql("""
			UPDATE asset_import_jobs
			SET status = :status,
			    total_rows = :totalRows,
			    processed_rows = :processedRows,
			    success_count = :successCount,
			    error_count = :errorCount,
			    result_summary_json = CAST(:summaryJson AS jsonb),
			    finished_at = :finishedAt
			WHERE id = :id
			  AND organization_id = :organizationId
			""")
			.param("id", job.id())
			.param("organizationId", job.organizationId())
			.param("status", job.status().name())
			.param("totalRows", job.totalRows())
			.param("processedRows", job.processedRows())
			.param("successCount", job.successCount())
			.param("errorCount", job.errorCount())
			.param("summaryJson", serializeSummary(job.summary()))
			.param("finishedAt", job.finishedAt())
			.update();

		return job;
	}

	@Override
	public Optional<AssetImportJob> findById(UUID jobId) {
		return jdbcClient.sql(baseSelect() + """
			WHERE id = :jobId
			""")
			.param("jobId", jobId)
			.query(this::mapJob)
			.optional();
	}

	@Override
	public Optional<AssetImportJob> findByIdAndOrganizationId(UUID jobId, UUID organizationId) {
		return jdbcClient.sql(baseSelect() + """
			WHERE id = :jobId
			  AND organization_id = :organizationId
			""")
			.param("jobId", jobId)
			.param("organizationId", organizationId)
			.query(this::mapJob)
			.optional();
	}

	private String baseSelect() {
		return """
			SELECT id, organization_id, uploaded_by_user_id, status, file_name, total_rows, processed_rows,
			       success_count, error_count, result_summary_json, started_at, finished_at, created_at
			FROM asset_import_jobs
			""";
	}

	private AssetImportJob mapJob(ResultSet resultSet, int rowNum) throws SQLException {
		return new AssetImportJob(
			resultSet.getObject("id", UUID.class),
			resultSet.getObject("organization_id", UUID.class),
			resultSet.getObject("uploaded_by_user_id", UUID.class),
			AssetImportJobStatus.valueOf(resultSet.getString("status")),
			resultSet.getString("file_name"),
			resultSet.getInt("total_rows"),
			resultSet.getInt("processed_rows"),
			resultSet.getInt("success_count"),
			resultSet.getInt("error_count"),
			deserializeSummary(resultSet.getString("result_summary_json")),
			resultSet.getObject("started_at", Instant.class),
			resultSet.getObject("finished_at", Instant.class),
			resultSet.getObject("created_at", Instant.class)
		);
	}

	private String serializeSummary(AssetImportSummary summary) {
		try {
			return objectMapper.writeValueAsString(summary == null ? new AssetImportSummary(java.util.List.of(), null) : summary);
		}
		catch (JsonProcessingException exception) {
			throw new IllegalStateException("Could not serialize asset import summary.", exception);
		}
	}

	private AssetImportSummary deserializeSummary(String json) {
		try {
			return json == null || json.isBlank()
				? new AssetImportSummary(java.util.List.of(), null)
				: objectMapper.readValue(json, AssetImportSummary.class);
		}
		catch (JsonProcessingException exception) {
			throw new IllegalStateException("Could not deserialize asset import summary.", exception);
		}
	}
}
