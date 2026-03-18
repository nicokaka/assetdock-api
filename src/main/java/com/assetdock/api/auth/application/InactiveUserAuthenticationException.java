package com.assetdock.api.auth.application;

public class InactiveUserAuthenticationException extends RuntimeException {

	public InactiveUserAuthenticationException() {
		super("Inactive users cannot authenticate.");
	}
}
