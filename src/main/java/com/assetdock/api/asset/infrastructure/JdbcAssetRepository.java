package com.assetdock.api.asset.infrastructure;

import com.assetdock.api.asset.domain.Asset;
import com.assetdock.api.asset.domain.AssetRepository;
import com.assetdock.api.asset.domain.AssetStatus;
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
				:status,
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
			.param("archivedAt", asset.archivedAt())
			.param("createdAt", asset.createdAt())
			.param("updatedAt", asset.updatedAt())
			.update();

		return asset;
	}

	@Override
	public List<Asset> findAllByOrganizationId(UUID organizationId) {
		return jdbcClient.sql(baseSelect() + """
			WHERE organization_id = :organizationId
			ORDER BY display_name, asset_tag
			""")
			.param("organizationId", organizationId)
			.query(this::mapAsset)
			.list();
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
			    status = :status,
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
			.param("archivedAt", asset.archivedAt())
			.param("updatedAt", asset.updatedAt())
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
			.param("archivedAt", archivedAt)
			.param("updatedAt", updatedAt)
			.update();

		return findByIdAndOrganizationId(assetId, organizationId).orElseThrow();
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
			resultSet.getObject("archived_at", Instant.class),
			resultSet.getObject("created_at", Instant.class),
			resultSet.getObject("updated_at", Instant.class)
		);
	}
}
