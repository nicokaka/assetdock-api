package com.assetdock.api.catalog.infrastructure;

import com.assetdock.api.catalog.domain.Location;
import com.assetdock.api.catalog.domain.LocationRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcLocationRepository implements LocationRepository {

	private final JdbcClient jdbcClient;

	public JdbcLocationRepository(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

	@Override
	public boolean existsByOrganizationIdAndName(UUID organizationId, String normalizedName) {
		Boolean exists = jdbcClient.sql("""
			SELECT EXISTS(
				SELECT 1
				FROM locations
				WHERE organization_id = :organizationId
				  AND LOWER(name) = :name
			)
			""")
			.param("organizationId", organizationId)
			.param("name", normalizedName)
			.query(Boolean.class)
			.single();

		return Boolean.TRUE.equals(exists);
	}

	@Override
	public Location save(Location location) {
		jdbcClient.sql("""
			INSERT INTO locations (id, organization_id, name, description, active, created_at, updated_at)
			VALUES (:id, :organizationId, :name, :description, :active, :createdAt, :updatedAt)
			""")
			.param("id", location.id())
			.param("organizationId", location.organizationId())
			.param("name", location.name())
			.param("description", location.description())
			.param("active", location.active())
			.param("createdAt", location.createdAt())
			.param("updatedAt", location.updatedAt())
			.update();

		return location;
	}

	@Override
	public Location update(Location location) {
		jdbcClient.sql("""
			UPDATE locations
			SET name = :name,
			    description = :description,
			    active = :active,
			    updated_at = :updatedAt
			WHERE id = :id
			  AND organization_id = :organizationId
			""")
			.param("id", location.id())
			.param("organizationId", location.organizationId())
			.param("name", location.name())
			.param("description", location.description())
			.param("active", location.active())
			.param("updatedAt", location.updatedAt())
			.update();

		return location;
	}

	@Override
	public List<Location> findAllByOrganizationId(UUID organizationId) {
		return jdbcClient.sql("""
			SELECT id, organization_id, name, description, active, created_at, updated_at
			FROM locations
			WHERE organization_id = :organizationId
			ORDER BY name
			""")
			.param("organizationId", organizationId)
			.query(this::mapLocation)
			.list();
	}

	@Override
	public Optional<Location> findByIdAndOrganizationId(UUID id, UUID organizationId) {
		return jdbcClient.sql("""
			SELECT id, organization_id, name, description, active, created_at, updated_at
			FROM locations
			WHERE id = :id
			  AND organization_id = :organizationId
			""")
			.param("id", id)
			.param("organizationId", organizationId)
			.query(this::mapLocation)
			.optional();
	}

	private Location mapLocation(ResultSet resultSet, int rowNum) throws SQLException {
		return new Location(
			resultSet.getObject("id", UUID.class),
			resultSet.getObject("organization_id", UUID.class),
			resultSet.getString("name"),
			resultSet.getString("description"),
			resultSet.getBoolean("active"),
			resultSet.getObject("created_at", Instant.class),
			resultSet.getObject("updated_at", Instant.class)
		);
	}
}
