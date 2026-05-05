package com.assetdock.api.user.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminResetPasswordRequest(
	@NotBlank(message = "New password is required.")
	@Size(min = 8, message = "New password must be at least 8 characters long.")
	String newPassword
) {
}
