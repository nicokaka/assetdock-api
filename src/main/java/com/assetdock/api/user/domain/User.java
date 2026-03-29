package com.assetdock.api.user.domain;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record User(
	UUID id,
	UUID organizationId,
	String email,
	String fullName,
	String passwordHash,
	UserStatus status,
	Set<UserRole> roles,
	int failedLoginAttempts,
	Instant lastLoginAt,
	Instant createdAt,
	Instant updatedAt
) {
}
