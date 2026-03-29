package com.assetdock.api.auth.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface WebSessionRepository {

	WebSession save(WebSession session);

	Optional<WebSession> findById(UUID sessionId);

	void updateLastActiveAt(UUID sessionId, Instant lastActiveAt);

	void invalidate(UUID sessionId, Instant invalidatedAt);
}
