package com.assetdock.api.user.application;

import com.assetdock.api.user.domain.UserRole;
import com.assetdock.api.user.domain.UserStatus;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserView(
	UUID id,
	UUID organizationId,
	String fullName,
	String email,
	UserStatus status,
	Set<UserRole> roles,
	Instant createdAt,
	Instant updatedAt
) {
}
