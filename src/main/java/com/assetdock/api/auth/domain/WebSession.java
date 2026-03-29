package com.assetdock.api.auth.domain;

import java.time.Instant;
import java.util.UUID;

public record WebSession(
	UUID id,
	UUID userId,
	String csrfToken,
	Instant createdAt,
	Instant lastActiveAt,
	Instant expiresAt,
	Instant invalidatedAt
) {
}
