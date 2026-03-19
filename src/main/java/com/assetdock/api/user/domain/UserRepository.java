package com.assetdock.api.user.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

	Optional<User> findByEmail(String normalizedEmail);

	Optional<User> findById(UUID userId);

	List<User> findAll();

	List<User> findAllByOrganizationId(UUID organizationId);

	boolean existsByEmail(String normalizedEmail);

	User save(User user);

	User updateStatus(UUID userId, UserStatus status, Instant updatedAt);

	void updateLastLoginAt(UUID userId, Instant lastLoginAt);
}
