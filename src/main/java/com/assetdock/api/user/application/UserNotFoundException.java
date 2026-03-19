package com.assetdock.api.user.application;

public class UserNotFoundException extends RuntimeException {

	public UserNotFoundException() {
		super("User not found.");
	}
}
