package com.assetdock.api.person.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePersonRequest(
	@NotBlank(message = "fullName must not be blank")
	@Size(max = 150, message = "fullName must not exceed 150 characters")
	String fullName,

	@Size(max = 320, message = "email must not exceed 320 characters")
	String email,

	@Size(max = 100, message = "department must not exceed 100 characters")
	String department,

	boolean active
) {
}
