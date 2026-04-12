package com.assetdock.api.user.application;

public record UpdateUserProfileCommand(
	String fullName,
	String email
) {
}
