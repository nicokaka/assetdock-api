package com.assetdock.api.assignment.application;

public class InvalidAssignmentRequestException extends RuntimeException {

	public InvalidAssignmentRequestException(String message) {
		super(message);
	}
}
