package com.assetdock.api.person.infrastructure;

import com.assetdock.api.person.domain.Person;
import com.assetdock.api.person.domain.PersonRepository;
import com.assetdock.api.common.infrastructure.JdbcColumnReaders;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcPersonRepository implements PersonRepository {

	private final JdbcClient jdbcClient;

	public JdbcPersonRepository(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

	@Override
	public boolean existsByOrganizationIdAndEmail(UUID organizationId, String email) {
		if (email == null || email.isBlank()) {
			return false;
		}
		Boolean exists = jdbcClient.sql("""
			SELECT EXISTS(
				SELECT 1
				FROM people
				WHERE organization_id = :organizationId
				  AND LOWER(email) = LOWER(:email)
			)
			""")
			.param("organizationId", organizationId)
			.param("email", email.trim())
			.query(Boolean.class)
			.single();

		return Boolean.TRUE.equals(exists);
	}

	@Override
	public Person save(Person person) {
		jdbcClient.sql("""
			INSERT INTO people (
				id,
				organization_id,
				full_name,
				email,
				department,
				active,
				created_at,
				updated_at
			)
			VALUES (
				:id,
				:organizationId,
				:fullName,
				:email,
				:department,
				:active,
				:createdAt,
				:updatedAt
			)
			""")
			.param("id", person.id())
			.param("organizationId", person.organizationId())
			.param("fullName", person.fullName().trim())
			.param("email", person.email() != null ? person.email().trim() : null)
			.param("department", person.department() != null ? person.department().trim() : null)
			.param("active", person.active())
			.param("createdAt", JdbcColumnReaders.toOffsetDateTime(person.createdAt()))
			.param("updatedAt", JdbcColumnReaders.toOffsetDateTime(person.updatedAt()))
			.update();

		return person;
	}

	@Override
	public List<Person> findAllPaginated(UUID organizationId, int limit, int offset, String search, Boolean active) {
		StringBuilder sql = new StringBuilder("SELECT id, organization_id, full_name, email, department, active, created_at, updated_at FROM people WHERE organization_id = :organizationId ");
		
		if (active != null) {
			sql.append(" AND active = :active ");
		}
		
		if (search != null && !search.isBlank()) {
			sql.append(" AND (full_name ILIKE :search OR email ILIKE :search OR department ILIKE :search) ");
		}
		
		sql.append(" ORDER BY full_name, id LIMIT :limit OFFSET :offset");

		var statement = jdbcClient.sql(sql.toString())
			.param("organizationId", organizationId)
			.param("limit", limit)
			.param("offset", offset);

		if (active != null) {
			statement = statement.param("active", active);
		}
		
		if (search != null && !search.isBlank()) {
			statement = statement.param("search", "%" + search + "%");
		}

		return statement.query(this::mapPerson).list();
	}

	@Override
	public long countForOrganization(UUID organizationId, String search, Boolean active) {
		StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM people WHERE organization_id = :organizationId ");
		
		if (active != null) {
			sql.append(" AND active = :active ");
		}
		
		if (search != null && !search.isBlank()) {
			sql.append(" AND (full_name ILIKE :search OR email ILIKE :search OR department ILIKE :search) ");
		}

		var statement = jdbcClient.sql(sql.toString())
			.param("organizationId", organizationId);

		if (active != null) {
			statement = statement.param("active", active);
		}
		
		if (search != null && !search.isBlank()) {
			statement = statement.param("search", "%" + search + "%");
		}

		Long count = statement.query(Long.class).single();
		return count == null ? 0 : count;
	}

	@Override
	public Optional<Person> findByIdAndOrganizationId(UUID personId, UUID organizationId) {
		return jdbcClient.sql("""
			SELECT id, organization_id, full_name, email, department, active, created_at, updated_at
			FROM people
			WHERE id = :personId
			  AND organization_id = :organizationId
			""")
			.param("personId", personId)
			.param("organizationId", organizationId)
			.query(this::mapPerson)
			.optional();
	}

	@Override
	public Optional<Person> findById(UUID personId) {
		return jdbcClient.sql("""
			SELECT id, organization_id, full_name, email, department, active, created_at, updated_at
			FROM people
			WHERE id = :personId
			""")
			.param("personId", personId)
			.query(this::mapPerson)
			.optional();
	}

	@Override
	public Person update(Person person) {
		jdbcClient.sql("""
			UPDATE people
			SET full_name = :fullName,
			    email = :email,
			    department = :department,
			    active = :active,
			    updated_at = :updatedAt
			WHERE id = :id
			  AND organization_id = :organizationId
			""")
			.param("id", person.id())
			.param("organizationId", person.organizationId())
			.param("fullName", person.fullName().trim())
			.param("email", person.email() != null ? person.email().trim() : null)
			.param("department", person.department() != null ? person.department().trim() : null)
			.param("active", person.active())
			.param("updatedAt", JdbcColumnReaders.toOffsetDateTime(person.updatedAt()))
			.update();

		return person;
	}

	private Person mapPerson(ResultSet resultSet, int rowNum) throws SQLException {
		return new Person(
			resultSet.getObject("id", UUID.class),
			resultSet.getObject("organization_id", UUID.class),
			resultSet.getString("full_name"),
			resultSet.getString("email"),
			resultSet.getString("department"),
			resultSet.getBoolean("active"),
			JdbcColumnReaders.getInstant(resultSet, "created_at"),
			JdbcColumnReaders.getInstant(resultSet, "updated_at")
		);
	}
}
