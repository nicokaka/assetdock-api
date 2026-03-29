package com.assetdock.api.organization.infrastructure;

import com.assetdock.api.organization.domain.Organization;
import com.assetdock.api.organization.domain.OrganizationRepository;
import com.assetdock.api.common.infrastructure.JdbcColumnReaders;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcOrganizationRepository implements OrganizationRepository {

	private final JdbcClient jdbcClient;

	public JdbcOrganizationRepository(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

	@Override
	public Optional<Organization> findById(UUID organizationId) {
		return jdbcClient.sql("""
			SELECT id, name, slug, created_at, updated_at
			FROM organizations
			WHERE id = :organizationId
			""")
			.param("organizationId", organizationId)
			.query(this::mapOrganization)
			.optional();
	}

	@Override
	public Optional<Organization> findBySlug(String slug) {
		return jdbcClient.sql("""
			SELECT id, name, slug, created_at, updated_at
			FROM organizations
			WHERE slug = :slug
			""")
			.param("slug", slug)
			.query(this::mapOrganization)
			.optional();
	}

	@Override
	public Organization save(Organization organization) {
		jdbcClient.sql("""
			INSERT INTO organizations (id, name, slug, created_at, updated_at)
			VALUES (:id, :name, :slug, :createdAt, :updatedAt)
			""")
			.param("id", organization.id())
			.param("name", organization.name())
			.param("slug", organization.slug())
			.param("createdAt", JdbcColumnReaders.toOffsetDateTime(organization.createdAt()))
			.param("updatedAt", JdbcColumnReaders.toOffsetDateTime(organization.updatedAt()))
			.update();

		return organization;
	}

	private Organization mapOrganization(ResultSet resultSet, int rowNum) throws SQLException {
		return new Organization(
			resultSet.getObject("id", UUID.class),
			resultSet.getString("name"),
			resultSet.getString("slug"),
			JdbcColumnReaders.getInstant(resultSet, "created_at"),
			JdbcColumnReaders.getInstant(resultSet, "updated_at")
		);
	}
}
