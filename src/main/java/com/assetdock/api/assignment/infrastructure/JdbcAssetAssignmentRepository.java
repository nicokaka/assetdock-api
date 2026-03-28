package com.assetdock.api.assignment.infrastructure;

import com.assetdock.api.assignment.domain.AssetAssignment;
import com.assetdock.api.assignment.domain.AssetAssignmentRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAssetAssignmentRepository implements AssetAssignmentRepository {

	private final JdbcClient jdbcClient;

	public JdbcAssetAssignmentRepository(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

	@Override
	public AssetAssignment save(AssetAssignment assignment) {
		jdbcClient.sql("""
			INSERT INTO asset_assignments (
				id,
				organization_id,
				asset_id,
				user_id,
				location_id,
				assigned_at,
				unassigned_at,
				assigned_by,
				notes,
				created_at
			)
			VALUES (
				:id,
				:organizationId,
				:assetId,
				:userId,
				:locationId,
				:assignedAt,
				:unassignedAt,
				:assignedBy,
				:notes,
				:createdAt
			)
			""")
			.param("id", assignment.id())
			.param("organizationId", assignment.organizationId())
			.param("assetId", assignment.assetId())
			.param("userId", assignment.userId())
			.param("locationId", assignment.locationId())
			.param("assignedAt", assignment.assignedAt())
			.param("unassignedAt", assignment.unassignedAt())
			.param("assignedBy", assignment.assignedBy())
			.param("notes", assignment.notes())
			.param("createdAt", assignment.createdAt())
			.update();

		return assignment;
	}

	@Override
	public Optional<AssetAssignment> findActiveByAssetIdAndOrganizationId(UUID assetId, UUID organizationId) {
		return jdbcClient.sql(baseSelect() + """
			WHERE asset_id = :assetId
			  AND organization_id = :organizationId
			  AND unassigned_at IS NULL
			ORDER BY assigned_at DESC
			LIMIT 1
			""")
			.param("assetId", assetId)
			.param("organizationId", organizationId)
			.query(this::mapAssignment)
			.optional();
	}

	@Override
	public List<AssetAssignment> findAllByAssetIdAndOrganizationId(UUID assetId, UUID organizationId, int limit) {
		return jdbcClient.sql(baseSelect() + """
			WHERE asset_id = :assetId
			  AND organization_id = :organizationId
			ORDER BY assigned_at DESC, created_at DESC
			LIMIT :limit
			""")
			.param("assetId", assetId)
			.param("organizationId", organizationId)
			.param("limit", limit)
			.query(this::mapAssignment)
			.list();
	}

	@Override
	public AssetAssignment closeActiveAssignment(UUID assignmentId, UUID organizationId, Instant unassignedAt) {
		jdbcClient.sql("""
			UPDATE asset_assignments
			SET unassigned_at = :unassignedAt
			WHERE id = :assignmentId
			  AND organization_id = :organizationId
			  AND unassigned_at IS NULL
			""")
			.param("unassignedAt", unassignedAt)
			.param("assignmentId", assignmentId)
			.param("organizationId", organizationId)
			.update();

		return jdbcClient.sql(baseSelect() + """
			WHERE id = :assignmentId
			  AND organization_id = :organizationId
			""")
			.param("assignmentId", assignmentId)
			.param("organizationId", organizationId)
			.query(this::mapAssignment)
			.single();
	}

	private String baseSelect() {
		return """
			SELECT id, organization_id, asset_id, user_id, location_id, assigned_at, unassigned_at, assigned_by, notes, created_at
			FROM asset_assignments
			""";
	}

	private AssetAssignment mapAssignment(ResultSet resultSet, int rowNum) throws SQLException {
		return new AssetAssignment(
			resultSet.getObject("id", UUID.class),
			resultSet.getObject("organization_id", UUID.class),
			resultSet.getObject("asset_id", UUID.class),
			resultSet.getObject("user_id", UUID.class),
			resultSet.getObject("location_id", UUID.class),
			resultSet.getObject("assigned_at", Instant.class),
			resultSet.getObject("unassigned_at", Instant.class),
			resultSet.getObject("assigned_by", UUID.class),
			resultSet.getString("notes"),
			resultSet.getObject("created_at", Instant.class)
		);
	}
}
