package com.assetdock.api.audit.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AuditLogRepository {

	void save(AuditLogEntry entry);

	long countByCriteria(
		UUID organizationId,
		AuditEventType eventType,
		Instant from,
		Instant to
	);

	List<AuditLogEntry> findByCriteria(
		UUID organizationId,
		AuditEventType eventType,
		Instant from,
		Instant to,
		int limit,
		int offset
	);
}
