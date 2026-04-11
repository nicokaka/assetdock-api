package com.assetdock.api.user.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequest(
	@NotBlank(message = "Full name is required.")
	@Size(max = 200, message = "Full name must not exceed 200 characters.")
	String fullName,

	@NotBlank(message = "Email is required.")
	@Email(message = "Email must be a valid address.")
	@Size(max = 254, message = "Email must not exceed 254 characters.")
	String email
) {
}
