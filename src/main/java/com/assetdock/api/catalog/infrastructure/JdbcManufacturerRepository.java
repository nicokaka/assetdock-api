package com.assetdock.api.catalog.infrastructure;

import com.assetdock.api.catalog.domain.Manufacturer;
import com.assetdock.api.catalog.domain.ManufacturerRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcManufacturerRepository implements ManufacturerRepository {

	private final JdbcClient jdbcClient;

	public JdbcManufacturerRepository(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

	@Override
	public boolean existsByOrganizationIdAndName(UUID organizationId, String normalizedName) {
		Boolean exists = jdbcClient.sql("""
			SELECT EXISTS(
				SELECT 1
				FROM manufacturers
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
	public Manufacturer save(Manufacturer manufacturer) {
		jdbcClient.sql("""
			INSERT INTO manufacturers (id, organization_id, name, description, website, active, created_at, updated_at)
			VALUES (:id, :organizationId, :name, :description, :website, :active, :createdAt, :updatedAt)
			""")
			.param("id", manufacturer.id())
			.param("organizationId", manufacturer.organizationId())
			.param("name", manufacturer.name())
			.param("description", manufacturer.description())
			.param("website", manufacturer.website())
			.param("active", manufacturer.active())
			.param("createdAt", manufacturer.createdAt())
			.param("updatedAt", manufacturer.updatedAt())
			.update();

		return manufacturer;
	}

	@Override
	public Manufacturer update(Manufacturer manufacturer) {
		jdbcClient.sql("""
			UPDATE manufacturers
			SET name = :name,
			    description = :description,
			    website = :website,
			    active = :active,
			    updated_at = :updatedAt
			WHERE id = :id
			  AND organization_id = :organizationId
			""")
			.param("id", manufacturer.id())
			.param("organizationId", manufacturer.organizationId())
			.param("name", manufacturer.name())
			.param("description", manufacturer.description())
			.param("website", manufacturer.website())
			.param("active", manufacturer.active())
			.param("updatedAt", manufacturer.updatedAt())
			.update();

		return manufacturer;
	}

	@Override
	public List<Manufacturer> findAllByOrganizationId(UUID organizationId) {
		return jdbcClient.sql("""
			SELECT id, organization_id, name, description, website, active, created_at, updated_at
			FROM manufacturers
			WHERE organization_id = :organizationId
			ORDER BY name
			""")
			.param("organizationId", organizationId)
			.query(this::mapManufacturer)
			.list();
	}

	@Override
	public Optional<Manufacturer> findByIdAndOrganizationId(UUID id, UUID organizationId) {
		return jdbcClient.sql("""
			SELECT id, organization_id, name, description, website, active, created_at, updated_at
			FROM manufacturers
			WHERE id = :id
			  AND organization_id = :organizationId
			""")
			.param("id", id)
			.param("organizationId", organizationId)
			.query(this::mapManufacturer)
			.optional();
	}

	private Manufacturer mapManufacturer(ResultSet resultSet, int rowNum) throws SQLException {
		return new Manufacturer(
			resultSet.getObject("id", UUID.class),
			resultSet.getObject("organization_id", UUID.class),
			resultSet.getString("name"),
			resultSet.getString("description"),
			resultSet.getString("website"),
			resultSet.getBoolean("active"),
			resultSet.getObject("created_at", Instant.class),
			resultSet.getObject("updated_at", Instant.class)
		);
	}
}
