package com.assetdock.api.user.application;

public class EmailAlreadyInUseException extends RuntimeException {

	public EmailAlreadyInUseException() {
		super("Email is already in use.");
	}
}
