package com.assetdock.api.person.application;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidPersonRequestException extends RuntimeException {
	public InvalidPersonRequestException(String message) {
		super(message);
	}
}
