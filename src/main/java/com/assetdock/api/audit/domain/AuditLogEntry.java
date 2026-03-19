package com.assetdock.api.audit.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditLogEntry(
	UUID id,
	UUID organizationId,
	UUID actorUserId,
	AuditEventType eventType,
	String resourceType,
	UUID resourceId,
	String outcome,
	String ipAddress,
	String userAgent,
	String requestId,
	Map<String, Object> details,
	Instant occurredAt
) {
}
