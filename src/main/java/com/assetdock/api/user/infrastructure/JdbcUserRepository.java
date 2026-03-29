package com.assetdock.api.user.infrastructure;

import com.assetdock.api.user.domain.User;
import com.assetdock.api.user.domain.UserRepository;
import com.assetdock.api.user.domain.UserRole;
import com.assetdock.api.user.domain.UserStatus;
import com.assetdock.api.common.infrastructure.JdbcColumnReaders;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcUserRepository implements UserRepository {

	private final JdbcClient jdbcClient;

	public JdbcUserRepository(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

	@Override
	public Optional<User> findByEmail(String normalizedEmail) {
		Optional<UserSnapshot> snapshot = jdbcClient.sql(baseSelect() + """
			WHERE LOWER(email) = :email
			""")
			.param("email", normalizedEmail)
			.query(this::mapUserSnapshot)
			.optional();

		return snapshot.map(this::toUser);
	}

	@Override
	public Optional<User> findById(UUID userId) {
		Optional<UserSnapshot> snapshot = jdbcClient.sql(baseSelect() + """
			WHERE id = :userId
			""")
			.param("userId", userId)
			.query(this::mapUserSnapshot)
			.optional();

		return snapshot.map(this::toUser);
	}

	@Override
	public List<User> findAll(int limit) {
		return jdbcClient.sql(baseSelect() + """
			ORDER BY full_name, email
			LIMIT :limit
			""")
			.param("limit", limit)
			.query(this::mapUserSnapshot)
			.list()
			.stream()
			.map(this::toUser)
			.toList();
	}

	@Override
	public List<User> findAllByOrganizationId(UUID organizationId, int limit) {
		return jdbcClient.sql(baseSelect() + """
			WHERE organization_id = :organizationId
			ORDER BY full_name, email
			LIMIT :limit
			""")
			.param("organizationId", organizationId)
			.param("limit", limit)
			.query(this::mapUserSnapshot)
			.list()
			.stream()
			.map(this::toUser)
			.toList();
	}

	@Override
	public boolean existsByEmail(String normalizedEmail) {
		Boolean exists = jdbcClient.sql("""
			SELECT EXISTS(
				SELECT 1
				FROM users
				WHERE LOWER(email) = :email
			)
			""")
			.param("email", normalizedEmail)
			.query(Boolean.class)
			.single();

		return Boolean.TRUE.equals(exists);
	}

	@Override
	public User save(User user) {
		jdbcClient.sql("""
			INSERT INTO users (id, organization_id, email, full_name, password_hash, status, last_login_at, created_at, updated_at)
			VALUES (:id, :organizationId, :email, :fullName, :passwordHash, CAST(:status AS user_status), :lastLoginAt, :createdAt, :updatedAt)
			""")
			.param("id", user.id())
			.param("organizationId", user.organizationId())
			.param("email", user.email())
			.param("fullName", user.fullName())
			.param("passwordHash", user.passwordHash())
			.param("status", user.status().name())
			.param("lastLoginAt", JdbcColumnReaders.toOffsetDateTime(user.lastLoginAt()))
			.param("createdAt", JdbcColumnReaders.toOffsetDateTime(user.createdAt()))
			.param("updatedAt", JdbcColumnReaders.toOffsetDateTime(user.updatedAt()))
			.update();

		user.roles().forEach(role -> jdbcClient.sql("""
			INSERT INTO user_roles (user_id, role, created_at)
			VALUES (:userId, CAST(:role AS user_role), :createdAt)
			""")
			.param("userId", user.id())
			.param("role", role.name())
				.param("createdAt", JdbcColumnReaders.toOffsetDateTime(user.createdAt()))
			.update());

		return user;
	}

	@Override
	public User updateStatus(UUID userId, UserStatus status, Instant updatedAt) {
		jdbcClient.sql("""
			UPDATE users
			SET status = CAST(:status AS user_status),
			    updated_at = :updatedAt
			WHERE id = :userId
			""")
			.param("status", status.name())
			.param("updatedAt", JdbcColumnReaders.toOffsetDateTime(updatedAt))
			.param("userId", userId)
			.update();

		return findById(userId).orElseThrow();
	}

	@Override
	public User updateRoles(UUID userId, Set<UserRole> roles, Instant updatedAt) {
		jdbcClient.sql("""
			UPDATE users
			SET updated_at = :updatedAt
			WHERE id = :userId
			""")
			.param("updatedAt", JdbcColumnReaders.toOffsetDateTime(updatedAt))
			.param("userId", userId)
			.update();

		jdbcClient.sql("""
			DELETE FROM user_roles
			WHERE user_id = :userId
			""")
			.param("userId", userId)
			.update();

		roles.forEach(role -> jdbcClient.sql("""
			INSERT INTO user_roles (user_id, role, created_at)
			VALUES (:userId, CAST(:role AS user_role), :createdAt)
			""")
			.param("userId", userId)
			.param("role", role.name())
				.param("createdAt", JdbcColumnReaders.toOffsetDateTime(updatedAt))
				.update());

		return findById(userId).orElseThrow();
	}

	@Override
	public User incrementFailedLoginAttempts(UUID userId, Instant updatedAt) {
		jdbcClient.sql("""
			UPDATE users
			SET failed_login_attempts = failed_login_attempts + 1,
			    updated_at = :updatedAt
			WHERE id = :userId
			""")
			.param("updatedAt", JdbcColumnReaders.toOffsetDateTime(updatedAt))
			.param("userId", userId)
			.update();

		return findById(userId).orElseThrow();
	}

	@Override
	public User resetFailedLoginAttempts(UUID userId, Instant updatedAt) {
		jdbcClient.sql("""
			UPDATE users
			SET failed_login_attempts = 0,
			    updated_at = :updatedAt
			WHERE id = :userId
			""")
			.param("updatedAt", JdbcColumnReaders.toOffsetDateTime(updatedAt))
			.param("userId", userId)
			.update();

		return findById(userId).orElseThrow();
	}

	@Override
	public long countActiveUsersByOrganizationIdAndRole(UUID organizationId, UserRole role) {
		Long count = jdbcClient.sql("""
			SELECT COUNT(*)
			FROM users u
			INNER JOIN user_roles ur ON ur.user_id = u.id
			WHERE u.organization_id = :organizationId
			  AND u.status = 'ACTIVE'
			  AND ur.role = CAST(:role AS user_role)
			""")
			.param("organizationId", organizationId)
			.param("role", role.name())
			.query(Long.class)
			.single();

		return count == null ? 0 : count;
	}

	@Override
	public long countActiveUsersByRole(UserRole role) {
		Long count = jdbcClient.sql("""
			SELECT COUNT(*)
			FROM users u
			INNER JOIN user_roles ur ON ur.user_id = u.id
			WHERE u.status = 'ACTIVE'
			  AND ur.role = CAST(:role AS user_role)
			""")
			.param("role", role.name())
			.query(Long.class)
			.single();

		return count == null ? 0 : count;
	}

	@Override
	public void updateLastLoginAt(UUID userId, Instant lastLoginAt) {
		jdbcClient.sql("""
			UPDATE users
			SET last_login_at = :lastLoginAt,
			    updated_at = :updatedAt
			WHERE id = :userId
			""")
			.param("lastLoginAt", JdbcColumnReaders.toOffsetDateTime(lastLoginAt))
			.param("updatedAt", JdbcColumnReaders.toOffsetDateTime(lastLoginAt))
			.param("userId", userId)
			.update();
	}

	private User toUser(UserSnapshot snapshot) {
		return new User(
			snapshot.id(),
			snapshot.organizationId(),
			snapshot.email(),
			snapshot.fullName(),
			snapshot.passwordHash(),
			snapshot.status(),
			loadRoles(snapshot.id()),
			snapshot.failedLoginAttempts(),
			snapshot.lastLoginAt(),
			snapshot.createdAt(),
			snapshot.updatedAt()
		);
	}

	private Set<UserRole> loadRoles(UUID userId) {
		return jdbcClient.sql("""
			SELECT role
			FROM user_roles
			WHERE user_id = :userId
			ORDER BY role
			""")
			.param("userId", userId)
			.query((resultSet, rowNum) -> UserRole.valueOf(resultSet.getString("role")))
			.list()
			.stream()
			.collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);
	}

	private String baseSelect() {
		return """
			SELECT id, organization_id, email, full_name, password_hash, status, failed_login_attempts, last_login_at, created_at, updated_at
			FROM users
			""";
	}

	private UserSnapshot mapUserSnapshot(ResultSet resultSet, int rowNum) throws SQLException {
		return new UserSnapshot(
			resultSet.getObject("id", UUID.class),
			resultSet.getObject("organization_id", UUID.class),
			resultSet.getString("email"),
			resultSet.getString("full_name"),
			resultSet.getString("password_hash"),
			UserStatus.valueOf(resultSet.getString("status")),
			resultSet.getInt("failed_login_attempts"),
			JdbcColumnReaders.getInstant(resultSet, "last_login_at"),
			JdbcColumnReaders.getInstant(resultSet, "created_at"),
			JdbcColumnReaders.getInstant(resultSet, "updated_at")
		);
	}

	private record UserSnapshot(
		UUID id,
		UUID organizationId,
		String email,
		String fullName,
		String passwordHash,
		UserStatus status,
		int failedLoginAttempts,
		Instant lastLoginAt,
		Instant createdAt,
		Instant updatedAt
	) {
	}
}
