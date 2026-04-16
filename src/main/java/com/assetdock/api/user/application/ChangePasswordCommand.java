package com.assetdock.api.user.application;

public record ChangePasswordCommand(
	String currentPassword,
	String newPassword
) {
}
