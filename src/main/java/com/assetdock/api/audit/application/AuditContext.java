package com.assetdock.api.audit.application;

public record AuditContext(
	String ipAddress,
	String userAgent,
	String requestId
) {
}
