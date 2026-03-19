package com.assetdock.api.user.application;

public class InvalidUserRequestException extends RuntimeException {

	public InvalidUserRequestException(String message) {
		super(message);
	}
}
