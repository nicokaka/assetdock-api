package com.assetdock.api.user.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequest(
	@NotBlank(message = "Full name is required.")
	@Size(max = 150, message = "Full name must not exceed 150 characters.")
	String fullName,

	@NotBlank(message = "Email is required.")
	@Email(message = "Email must be a valid address.")
	@Size(max = 320, message = "Email must not exceed 320 characters.")
	String email
) {
}
