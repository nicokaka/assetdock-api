package com.assetdock.api.catalog.infrastructure;

import com.assetdock.api.catalog.domain.Category;
import com.assetdock.api.catalog.domain.CategoryRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcCategoryRepository implements CategoryRepository {

	private final JdbcClient jdbcClient;

	public JdbcCategoryRepository(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

	@Override
	public boolean existsByOrganizationIdAndName(UUID organizationId, String normalizedName) {
		Boolean exists = jdbcClient.sql("""
			SELECT EXISTS(
				SELECT 1
				FROM categories
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
	public Category save(Category category) {
		jdbcClient.sql("""
			INSERT INTO categories (id, organization_id, name, description, active, created_at, updated_at)
			VALUES (:id, :organizationId, :name, :description, :active, :createdAt, :updatedAt)
			""")
			.param("id", category.id())
			.param("organizationId", category.organizationId())
			.param("name", category.name())
			.param("description", category.description())
			.param("active", category.active())
			.param("createdAt", category.createdAt())
			.param("updatedAt", category.updatedAt())
			.update();

		return category;
	}

	@Override
	public Category update(Category category) {
		jdbcClient.sql("""
			UPDATE categories
			SET name = :name,
			    description = :description,
			    active = :active,
			    updated_at = :updatedAt
			WHERE id = :id
			  AND organization_id = :organizationId
			""")
			.param("id", category.id())
			.param("organizationId", category.organizationId())
			.param("name", category.name())
			.param("description", category.description())
			.param("active", category.active())
			.param("updatedAt", category.updatedAt())
			.update();

		return category;
	}

	@Override
	public List<Category> findAllByOrganizationId(UUID organizationId) {
		return jdbcClient.sql("""
			SELECT id, organization_id, name, description, active, created_at, updated_at
			FROM categories
			WHERE organization_id = :organizationId
			ORDER BY name
			""")
			.param("organizationId", organizationId)
			.query(this::mapCategory)
			.list();
	}

	@Override
	public Optional<Category> findByIdAndOrganizationId(UUID id, UUID organizationId) {
		return jdbcClient.sql("""
			SELECT id, organization_id, name, description, active, created_at, updated_at
			FROM categories
			WHERE id = :id
			  AND organization_id = :organizationId
			""")
			.param("id", id)
			.param("organizationId", organizationId)
			.query(this::mapCategory)
			.optional();
	}

	private Category mapCategory(ResultSet resultSet, int rowNum) throws SQLException {
		return new Category(
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
