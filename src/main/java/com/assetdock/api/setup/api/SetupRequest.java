package com.assetdock.api.setup.api;

import com.assetdock.api.common.validation.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SetupRequest(
	@NotBlank(message = "Organization name is required.")
	@Size(min = 2, max = 200, message = "Organization name must be between 2 and 200 characters.")
	String organizationName,

	@NotBlank(message = "Admin full name is required.")
	@Size(min = 2, max = 200, message = "Admin full name must be between 2 and 200 characters.")
	String adminFullName,

	@NotBlank(message = "Admin email is required.")
	@Email(message = "A valid email address is required.")
	String adminEmail,

	@NotBlank(message = "Admin password is required.")
	@ValidPassword
	String adminPassword
) {
}
