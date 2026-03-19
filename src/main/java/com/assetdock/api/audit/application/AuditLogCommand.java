package com.assetdock.api.audit.application;

import com.assetdock.api.audit.domain.AuditEventType;
import java.util.Map;
import java.util.UUID;

public record AuditLogCommand(
	UUID organizationId,
	UUID actorUserId,
	AuditEventType eventType,
	String resourceType,
	UUID resourceId,
	String outcome,
	Map<String, Object> details
) {
}
