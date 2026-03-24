package com.assetdock.api.audit.application;

public class InvalidAuditLogQueryException extends RuntimeException {

	public InvalidAuditLogQueryException(String message) {
		super(message);
	}
}
