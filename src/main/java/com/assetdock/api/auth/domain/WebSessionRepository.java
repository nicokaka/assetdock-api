package com.assetdock.api.auth.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface WebSessionRepository {

	WebSession save(WebSession session);

	Optional<WebSession> findById(UUID sessionId);

	void updateLastActiveAt(UUID sessionId, Instant lastActiveAt);

	void invalidate(UUID sessionId, Instant invalidatedAt);

	/**
	 * Invalidates all active sessions for a user. Used when a password change
	 * requires all existing sessions to be revoked for security reasons.
	 */
	void invalidateAllByUserId(UUID userId, Instant invalidatedAt);
}
