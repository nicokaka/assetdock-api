package com.assetdock.api.asset.infrastructure;

import com.assetdock.api.asset.domain.Asset;
import com.assetdock.api.asset.domain.AssetRepository;
import com.assetdock.api.asset.domain.AssetStatus;
import com.assetdock.api.common.infrastructure.JdbcColumnReaders;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAssetRepository implements AssetRepository {

	private final JdbcClient jdbcClient;

	public JdbcAssetRepository(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

	@Override
	public boolean existsByOrganizationIdAndAssetTag(UUID organizationId, String normalizedAssetTag) {
		Boolean exists = jdbcClient.sql("""
			SELECT EXISTS(
				SELECT 1
				FROM assets
				WHERE organization_id = :organizationId
				  AND LOWER(asset_tag) = :assetTag
			)
			""")
			.param("organizationId", organizationId)
			.param("assetTag", normalizedAssetTag)
			.query(Boolean.class)
			.single();

		return Boolean.TRUE.equals(exists);
	}

	@Override
	public Asset save(Asset asset) {
		jdbcClient.sql("""
			INSERT INTO assets (
				id,
				organization_id,
				asset_tag,
				serial_number,
				hostname,
				display_name,
				description,
				category_id,
				manufacturer_id,
				current_location_id,
				current_assigned_user_id,
				status,
				purchase_date,
				warranty_expiry_date,
				archived_at,
				created_at,
				updated_at
			)
			VALUES (
				:id,
				:organizationId,
				:assetTag,
				:serialNumber,
				:hostname,
				:displayName,
				:description,
				:categoryId,
				:manufacturerId,
				:currentLocationId,
				:currentAssignedUserId,
				CAST(:status AS asset_status),
				:purchaseDate,
				:warrantyExpiryDate,
				:archivedAt,
				:createdAt,
				:updatedAt
			)
			""")
			.param("id", asset.id())
			.param("organizationId", asset.organizationId())
			.param("assetTag", asset.assetTag())
			.param("serialNumber", asset.serialNumber())
			.param("hostname", asset.hostname())
			.param("displayName", asset.displayName())
			.param("description", asset.description())
			.param("categoryId", asset.categoryId())
			.param("manufacturerId", asset.manufacturerId())
			.param("currentLocationId", asset.currentLocationId())
			.param("currentAssignedUserId", asset.currentAssignedUserId())
			.param("status", asset.status().name())
			.param("purchaseDate", asset.purchaseDate())
			.param("warrantyExpiryDate", asset.warrantyExpiryDate())
			.param("archivedAt", JdbcColumnReaders.toOffsetDateTime(asset.archivedAt()))
			.param("createdAt", JdbcColumnReaders.toOffsetDateTime(asset.createdAt()))
			.param("updatedAt", JdbcColumnReaders.toOffsetDateTime(asset.updatedAt()))
			.update();

		return asset;
	}

	@Override
	public List<Asset> findAllPaginated(UUID organizationId, int limit, int offset, String status, String search) {
		StringBuilder sql = new StringBuilder(baseSelect() + " WHERE organization_id = :organizationId ");
		
		if (status != null && !status.isBlank()) {
			sql.append(" AND status = CAST(:status AS asset_status) ");
		}
		
		if (search != null && !search.isBlank()) {
			sql.append(" AND (asset_tag ILIKE :search OR display_name ILIKE :search OR serial_number ILIKE :search OR hostname ILIKE :search) ");
		}
		
		sql.append(" ORDER BY display_name, asset_tag LIMIT :limit OFFSET :offset");

		var statement = jdbcClient.sql(sql.toString())
			.param("organizationId", organizationId)
			.param("limit", limit)
			.param("offset", offset);

		if (status != null && !status.isBlank()) {
			statement = statement.param("status", status);
		}
		
		if (search != null && !search.isBlank()) {
			statement = statement.param("search", "%" + search + "%");
		}

		return statement.query(this::mapAsset).list();
	}

	@Override
	public long countForOrganization(UUID organizationId, String status, String search) {
		StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM assets WHERE organization_id = :organizationId ");
		
		if (status != null && !status.isBlank()) {
			sql.append(" AND status = CAST(:status AS asset_status) ");
		}
		
		if (search != null && !search.isBlank()) {
			sql.append(" AND (asset_tag ILIKE :search OR display_name ILIKE :search OR serial_number ILIKE :search OR hostname ILIKE :search) ");
		}

		var statement = jdbcClient.sql(sql.toString())
			.param("organizationId", organizationId);

		if (status != null && !status.isBlank()) {
			statement = statement.param("status", status);
		}
		
		if (search != null && !search.isBlank()) {
			statement = statement.param("search", "%" + search + "%");
		}

		Long count = statement.query(Long.class).single();
		return count == null ? 0 : count;
	}

	@Override
	public Optional<Asset> findByIdAndOrganizationId(UUID assetId, UUID organizationId) {
		return jdbcClient.sql(baseSelect() + """
			WHERE id = :assetId
			  AND organization_id = :organizationId
			""")
			.param("assetId", assetId)
			.param("organizationId", organizationId)
			.query(this::mapAsset)
			.optional();
	}

	@Override
	public Optional<Asset> findById(UUID assetId) {
		return jdbcClient.sql(baseSelect() + """
			WHERE id = :assetId
			""")
			.param("assetId", assetId)
			.query(this::mapAsset)
			.optional();
	}

	@Override
	public Asset update(Asset asset) {
		jdbcClient.sql("""
			UPDATE assets
			SET asset_tag = :assetTag,
			    serial_number = :serialNumber,
			    hostname = :hostname,
			    display_name = :displayName,
			    description = :description,
			    category_id = :categoryId,
			    manufacturer_id = :manufacturerId,
			    current_location_id = :currentLocationId,
			    current_assigned_user_id = :currentAssignedUserId,
			    status = CAST(:status AS asset_status),
			    purchase_date = :purchaseDate,
			    warranty_expiry_date = :warrantyExpiryDate,
			    archived_at = :archivedAt,
			    updated_at = :updatedAt
			WHERE id = :id
			  AND organization_id = :organizationId
			""")
			.param("id", asset.id())
			.param("organizationId", asset.organizationId())
			.param("assetTag", asset.assetTag())
			.param("serialNumber", asset.serialNumber())
			.param("hostname", asset.hostname())
			.param("displayName", asset.displayName())
			.param("description", asset.description())
			.param("categoryId", asset.categoryId())
			.param("manufacturerId", asset.manufacturerId())
			.param("currentLocationId", asset.currentLocationId())
			.param("currentAssignedUserId", asset.currentAssignedUserId())
			.param("status", asset.status().name())
			.param("purchaseDate", asset.purchaseDate())
			.param("warrantyExpiryDate", asset.warrantyExpiryDate())
			.param("archivedAt", JdbcColumnReaders.toOffsetDateTime(asset.archivedAt()))
			.param("updatedAt", JdbcColumnReaders.toOffsetDateTime(asset.updatedAt()))
			.update();

		return asset;
	}

	@Override
	public Asset archive(UUID assetId, UUID organizationId, Instant archivedAt, Instant updatedAt) {
		jdbcClient.sql("""
			UPDATE assets
			SET archived_at = :archivedAt,
			    updated_at = :updatedAt
			WHERE id = :assetId
			  AND organization_id = :organizationId
			""")
			.param("assetId", assetId)
			.param("organizationId", organizationId)
			.param("archivedAt", JdbcColumnReaders.toOffsetDateTime(archivedAt))
			.param("updatedAt", JdbcColumnReaders.toOffsetDateTime(updatedAt))
			.update();

		return findByIdAndOrganizationId(assetId, organizationId).orElseThrow();
	}

	@Override
	public java.util.Map<AssetStatus, Integer> countByStatusForOrganization(UUID organizationId) {
		return jdbcClient.sql("""
			SELECT status, COUNT(*) as count
			FROM assets
			WHERE organization_id = :organizationId
			  AND archived_at IS NULL
			GROUP BY status
			""")
			.param("organizationId", organizationId)
			.query(rs -> {
				java.util.Map<AssetStatus, Integer> counts = new java.util.EnumMap<>(AssetStatus.class);
				while (rs.next()) {
					counts.put(AssetStatus.valueOf(rs.getString("status")), rs.getInt("count"));
				}
				return counts;
			});
	}

	private String baseSelect() {
		return """
			SELECT id, organization_id, asset_tag, serial_number, hostname, display_name, description,
			       category_id, manufacturer_id, current_location_id, current_assigned_user_id, status,
			       purchase_date, warranty_expiry_date, archived_at, created_at, updated_at
			FROM assets
			""";
	}

	private Asset mapAsset(ResultSet resultSet, int rowNum) throws SQLException {
		return new Asset(
			resultSet.getObject("id", UUID.class),
			resultSet.getObject("organization_id", UUID.class),
			resultSet.getString("asset_tag"),
			resultSet.getString("serial_number"),
			resultSet.getString("hostname"),
			resultSet.getString("display_name"),
			resultSet.getString("description"),
			resultSet.getObject("category_id", UUID.class),
			resultSet.getObject("manufacturer_id", UUID.class),
			resultSet.getObject("current_location_id", UUID.class),
			resultSet.getObject("current_assigned_user_id", UUID.class),
			AssetStatus.valueOf(resultSet.getString("status")),
			resultSet.getObject("purchase_date", LocalDate.class),
			resultSet.getObject("warranty_expiry_date", LocalDate.class),
			JdbcColumnReaders.getInstant(resultSet, "archived_at"),
			JdbcColumnReaders.getInstant(resultSet, "created_at"),
			JdbcColumnReaders.getInstant(resultSet, "updated_at")
		);
	}
}
