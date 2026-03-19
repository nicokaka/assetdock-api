package com.assetdock.api.user.api;

import com.assetdock.api.user.domain.UserRole;
import com.assetdock.api.user.domain.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;

public record CreateUserRequest(
	UUID organizationId,
	@NotBlank @Size(max = 150) String fullName,
	@NotBlank @Email @Size(max = 320) String email,
	@NotBlank @Size(min = 8, max = 72) String password,
	@NotEmpty Set<UserRole> roles,
	@NotNull UserStatus status
) {
}
