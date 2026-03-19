package com.assetdock.api.user.application;

import com.assetdock.api.user.domain.UserRole;
import com.assetdock.api.user.domain.UserStatus;
import java.util.Set;
import java.util.UUID;

public record CreateUserCommand(
	UUID organizationId,
	String fullName,
	String email,
	String password,
	Set<UserRole> roles,
	UserStatus status
) {
}
