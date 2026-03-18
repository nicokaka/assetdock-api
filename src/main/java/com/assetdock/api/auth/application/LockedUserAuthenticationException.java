package com.assetdock.api.auth.application;

public class LockedUserAuthenticationException extends RuntimeException {

	public LockedUserAuthenticationException() {
		super("Locked users cannot authenticate.");
	}
}
