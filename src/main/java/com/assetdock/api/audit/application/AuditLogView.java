package com.assetdock.api.audit.application;

import com.assetdock.api.audit.domain.AuditEventType;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditLogView(
	UUID id,
	UUID organizationId,
	UUID actorUserId,
	AuditEventType eventType,
	String resourceType,
	UUID resourceId,
	String outcome,
	String requestId,
	Map<String, Object> details,
	Instant occurredAt
) {
}
