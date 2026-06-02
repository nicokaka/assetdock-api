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
	public List<Asset> findAllPaginated(UUID organizationId, int limit, int offset, String status, String search, UUID categoryId, UUID locationId) {
		StringBuilder sql = new StringBuilder(baseSelect() + " WHERE a.organization_id = :organizationId ");
		
		if (status != null && !status.isBlank()) {
			if ("OPERATIONAL".equalsIgnoreCase(status)) {
				sql.append(" AND a.status IN (CAST('ASSIGNED' AS asset_status), CAST('IN_STOCK' AS asset_status)) ");
			} else {
				sql.append(" AND a.status = CAST(:status AS asset_status) ");
			}
		}
		
		if (search != null && !search.isBlank()) {
			sql.append(" AND (a.asset_tag ILIKE :search OR a.display_name ILIKE :search OR a.serial_number ILIKE :search OR a.hostname ILIKE :search) ");
		}

		if (categoryId != null) {
			sql.append(" AND a.category_id = :categoryId ");
		}

		if (locationId != null) {
			sql.append(" AND a.current_location_id = :locationId ");
		}
		
		sql.append(" ORDER BY a.display_name, a.asset_tag LIMIT :limit OFFSET :offset");

		var statement = jdbcClient.sql(sql.toString())
			.param("organizationId", organizationId)
			.param("limit", limit)
			.param("offset", offset);

		if (status != null && !status.isBlank() && !"OPERATIONAL".equalsIgnoreCase(status)) {
			statement = statement.param("status", status);
		}
		
		if (search != null && !search.isBlank()) {
			statement = statement.param("search", "%" + search + "%");
		}

		if (categoryId != null) {
			statement = statement.param("categoryId", categoryId);
		}

		if (locationId != null) {
			statement = statement.param("locationId", locationId);
		}

		return statement.query(this::mapAsset).list();
	}

	@Override
	public long countForOrganization(UUID organizationId, String status, String search, UUID categoryId, UUID locationId) {
		StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM assets WHERE organization_id = :organizationId ");
		
		if (status != null && !status.isBlank()) {
			if ("OPERATIONAL".equalsIgnoreCase(status)) {
				sql.append(" AND status IN (CAST('ASSIGNED' AS asset_status), CAST('IN_STOCK' AS asset_status)) ");
			} else {
				sql.append(" AND status = CAST(:status AS asset_status) ");
			}
		}
		
		if (search != null && !search.isBlank()) {
			sql.append(" AND (asset_tag ILIKE :search OR display_name ILIKE :search OR serial_number ILIKE :search OR hostname ILIKE :search) ");
		}

		if (categoryId != null) {
			sql.append(" AND category_id = :categoryId ");
		}

		if (locationId != null) {
			sql.append(" AND current_location_id = :locationId ");
		}

		var statement = jdbcClient.sql(sql.toString())
			.param("organizationId", organizationId);

		if (status != null && !status.isBlank() && !"OPERATIONAL".equalsIgnoreCase(status)) {
			statement = statement.param("status", status);
		}
		
		if (search != null && !search.isBlank()) {
			statement = statement.param("search", "%" + search + "%");
		}

		if (categoryId != null) {
			statement = statement.param("categoryId", categoryId);
		}

		if (locationId != null) {
			statement = statement.param("locationId", locationId);
		}

		Long count = statement.query(Long.class).single();
		return count == null ? 0 : count;
	}

	@Override
	public List<Asset> findAllPaginatedGlobally(int limit, int offset, String status, String search, UUID categoryId, UUID locationId) {
		StringBuilder sql = new StringBuilder(baseSelect() + " WHERE 1=1 ");
		
		if (status != null && !status.isBlank()) {
			if ("OPERATIONAL".equalsIgnoreCase(status)) {
				sql.append(" AND a.status IN (CAST('ASSIGNED' AS asset_status), CAST('IN_STOCK' AS asset_status)) ");
			} else {
				sql.append(" AND a.status = CAST(:status AS asset_status) ");
			}
		}
		
		if (search != null && !search.isBlank()) {
			sql.append(" AND (a.asset_tag ILIKE :search OR a.display_name ILIKE :search OR a.serial_number ILIKE :search OR a.hostname ILIKE :search) ");
		}

		if (categoryId != null) {
			sql.append(" AND a.category_id = :categoryId ");
		}

		if (locationId != null) {
			sql.append(" AND a.current_location_id = :locationId ");
		}
		
		sql.append(" ORDER BY a.display_name, a.asset_tag LIMIT :limit OFFSET :offset");

		var statement = jdbcClient.sql(sql.toString())
			.param("limit", limit)
			.param("offset", offset);

		if (status != null && !status.isBlank() && !"OPERATIONAL".equalsIgnoreCase(status)) {
			statement = statement.param("status", status);
		}
		
		if (search != null && !search.isBlank()) {
			statement = statement.param("search", "%" + search + "%");
		}

		if (categoryId != null) {
			statement = statement.param("categoryId", categoryId);
		}

		if (locationId != null) {
			statement = statement.param("locationId", locationId);
		}

		return statement.query(this::mapAsset).list();
	}

	@Override
	public long countGlobally(String status, String search, UUID categoryId, UUID locationId) {
		StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM assets WHERE 1=1 ");
		
		if (status != null && !status.isBlank()) {
			if ("OPERATIONAL".equalsIgnoreCase(status)) {
				sql.append(" AND status IN (CAST('ASSIGNED' AS asset_status), CAST('IN_STOCK' AS asset_status)) ");
			} else {
				sql.append(" AND status = CAST(:status AS asset_status) ");
			}
		}
		
		if (search != null && !search.isBlank()) {
			sql.append(" AND (asset_tag ILIKE :search OR display_name ILIKE :search OR serial_number ILIKE :search OR hostname ILIKE :search) ");
		}

		if (categoryId != null) {
			sql.append(" AND category_id = :categoryId ");
		}

		if (locationId != null) {
			sql.append(" AND current_location_id = :locationId ");
		}

		var statement = jdbcClient.sql(sql.toString());

		if (status != null && !status.isBlank() && !"OPERATIONAL".equalsIgnoreCase(status)) {
			statement = statement.param("status", status);
		}
		
		if (search != null && !search.isBlank()) {
			statement = statement.param("search", "%" + search + "%");
		}

		if (categoryId != null) {
			statement = statement.param("categoryId", categoryId);
		}

		if (locationId != null) {
			statement = statement.param("locationId", locationId);
		}

		Long count = statement.query(Long.class).single();
		return count == null ? 0 : count;
	}

	@Override
	public Optional<Asset> findByIdAndOrganizationId(UUID assetId, UUID organizationId) {
		return jdbcClient.sql(baseSelect() + """
			WHERE a.id = :assetId
			  AND a.organization_id = :organizationId
			""")
			.param("assetId", assetId)
			.param("organizationId", organizationId)
			.query(this::mapAsset)
			.optional();
	}

	@Override
	public Optional<Asset> findByIdAndOrganizationIdForUpdate(UUID assetId, UUID organizationId) {
		return jdbcClient.sql(baseSelect() + """
			WHERE a.id = :assetId
			  AND a.organization_id = :organizationId
			FOR UPDATE
			""")
			.param("assetId", assetId)
			.param("organizationId", organizationId)
			.query(this::mapAsset)
			.optional();
	}

	@Override
	public Optional<Asset> findById(UUID assetId) {
		return jdbcClient.sql(baseSelect() + """
			WHERE a.id = :assetId
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
	public void updateStatusAndAssignedUser(UUID assetId, UUID organizationId, AssetStatus status, UUID assignedUserId, Instant updatedAt) {
		jdbcClient.sql("""
			UPDATE assets
			SET status = CAST(:status AS asset_status),
			    current_assigned_user_id = :assignedUserId,
			    updated_at = :updatedAt
			WHERE id = :assetId
			  AND organization_id = :organizationId
			""")
			.param("assetId", assetId)
			.param("organizationId", organizationId)
			.param("status", status.name())
			.param("assignedUserId", assignedUserId)
			.param("updatedAt", JdbcColumnReaders.toOffsetDateTime(updatedAt))
			.update();
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
			SELECT a.id, a.organization_id, a.asset_tag, a.serial_number, a.hostname, a.display_name, a.description,
			       a.category_id, a.manufacturer_id, a.current_location_id, a.current_assigned_user_id, a.status,
			       a.purchase_date, a.warranty_expiry_date, a.archived_at, a.created_at, a.updated_at,
			       u.full_name AS current_assigned_user_name
			FROM assets a
			LEFT JOIN users u ON a.current_assigned_user_id = u.id
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
			resultSet.getString("current_assigned_user_name"),
			AssetStatus.valueOf(resultSet.getString("status")),
			resultSet.getObject("purchase_date", LocalDate.class),
			resultSet.getObject("warranty_expiry_date", LocalDate.class),
			JdbcColumnReaders.getInstant(resultSet, "archived_at"),
			JdbcColumnReaders.getInstant(resultSet, "created_at"),
			JdbcColumnReaders.getInstant(resultSet, "updated_at")
		);
	}
}
