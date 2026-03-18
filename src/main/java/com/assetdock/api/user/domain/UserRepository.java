package com.assetdock.api.user.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

	Optional<User> findByEmail(String normalizedEmail);

	void updateLastLoginAt(UUID userId, Instant lastLoginAt);
}
