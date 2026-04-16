package com.assetdock.api.auth.infrastructure;

import com.assetdock.api.auth.domain.WebSession;
import com.assetdock.api.auth.domain.WebSessionRepository;
import com.assetdock.api.common.infrastructure.JdbcColumnReaders;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcWebSessionRepository implements WebSessionRepository {

	private final JdbcClient jdbcClient;

	public JdbcWebSessionRepository(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

	@Override
	public WebSession save(WebSession session) {
		jdbcClient.sql("""
			INSERT INTO web_sessions (
				id,
				user_id,
				csrf_token,
				created_at,
				last_active_at,
				expires_at,
				invalidated_at
			) VALUES (
				:id,
				:userId,
				:csrfToken,
				:createdAt,
				:lastActiveAt,
				:expiresAt,
				:invalidatedAt
			)
			""")
			.param("id", session.id())
			.param("userId", session.userId())
			.param("csrfToken", session.csrfToken())
			.param("createdAt", JdbcColumnReaders.toOffsetDateTime(session.createdAt()))
			.param("lastActiveAt", JdbcColumnReaders.toOffsetDateTime(session.lastActiveAt()))
			.param("expiresAt", JdbcColumnReaders.toOffsetDateTime(session.expiresAt()))
			.param("invalidatedAt", JdbcColumnReaders.toOffsetDateTime(session.invalidatedAt()))
			.update();

		return session;
	}

	@Override
	public Optional<WebSession> findById(UUID sessionId) {
		return jdbcClient.sql("""
			SELECT id, user_id, csrf_token, created_at, last_active_at, expires_at, invalidated_at
			FROM web_sessions
			WHERE id = :sessionId
			""")
			.param("sessionId", sessionId)
			.query(this::mapSession)
			.optional();
	}

	@Override
	public void updateLastActiveAt(UUID sessionId, Instant lastActiveAt) {
		jdbcClient.sql("""
			UPDATE web_sessions
			SET last_active_at = :lastActiveAt
			WHERE id = :sessionId
			  AND invalidated_at IS NULL
			""")
			.param("sessionId", sessionId)
			.param("lastActiveAt", JdbcColumnReaders.toOffsetDateTime(lastActiveAt))
			.update();
	}

	@Override
	public void invalidate(UUID sessionId, Instant invalidatedAt) {
		jdbcClient.sql("""
			UPDATE web_sessions
			SET invalidated_at = COALESCE(invalidated_at, :invalidatedAt)
			WHERE id = :sessionId
			""")
			.param("sessionId", sessionId)
			.param("invalidatedAt", JdbcColumnReaders.toOffsetDateTime(invalidatedAt))
			.update();
	}

	@Override
	public void invalidateAllByUserId(UUID userId, Instant invalidatedAt) {
		jdbcClient.sql("""
			UPDATE web_sessions
			SET invalidated_at = :invalidatedAt
			WHERE user_id = :userId
			  AND invalidated_at IS NULL
			""")
			.param("userId", userId)
			.param("invalidatedAt", JdbcColumnReaders.toOffsetDateTime(invalidatedAt))
			.update();
	}

	private WebSession mapSession(ResultSet resultSet, int rowNum) throws SQLException {
		return new WebSession(
			resultSet.getObject("id", UUID.class),
			resultSet.getObject("user_id", UUID.class),
			resultSet.getString("csrf_token"),
			JdbcColumnReaders.getInstant(resultSet, "created_at"),
			JdbcColumnReaders.getInstant(resultSet, "last_active_at"),
			JdbcColumnReaders.getInstant(resultSet, "expires_at"),
			JdbcColumnReaders.getInstant(resultSet, "invalidated_at")
		);
	}
}
