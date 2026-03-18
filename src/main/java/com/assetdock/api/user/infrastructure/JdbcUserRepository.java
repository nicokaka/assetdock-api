package com.assetdock.api.user.infrastructure;

import com.assetdock.api.user.domain.User;
import com.assetdock.api.user.domain.UserRepository;
import com.assetdock.api.user.domain.UserRole;
import com.assetdock.api.user.domain.UserStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.LinkedHashSet;
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
		Optional<UserSnapshot> snapshot = jdbcClient.sql("""
			SELECT id, organization_id, email, full_name, password_hash, status, last_login_at, created_at, updated_at
			FROM users
			WHERE LOWER(email) = :email
			""")
			.param("email", normalizedEmail)
			.query(this::mapUserSnapshot)
			.optional();

		return snapshot.map(this::toUser);
	}

	@Override
	public void updateLastLoginAt(UUID userId, Instant lastLoginAt) {
		jdbcClient.sql("""
			UPDATE users
			SET last_login_at = :lastLoginAt,
			    updated_at = :updatedAt
			WHERE id = :userId
			""")
			.param("lastLoginAt", lastLoginAt)
			.param("updatedAt", lastLoginAt)
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

	private UserSnapshot mapUserSnapshot(ResultSet resultSet, int rowNum) throws SQLException {
		return new UserSnapshot(
			resultSet.getObject("id", UUID.class),
			resultSet.getObject("organization_id", UUID.class),
			resultSet.getString("email"),
			resultSet.getString("full_name"),
			resultSet.getString("password_hash"),
			UserStatus.valueOf(resultSet.getString("status")),
			resultSet.getObject("last_login_at", Instant.class),
			resultSet.getObject("created_at", Instant.class),
			resultSet.getObject("updated_at", Instant.class)
		);
	}

	private record UserSnapshot(
		UUID id,
		UUID organizationId,
		String email,
		String fullName,
		String passwordHash,
		UserStatus status,
		Instant lastLoginAt,
		Instant createdAt,
		Instant updatedAt
	) {
	}
}
