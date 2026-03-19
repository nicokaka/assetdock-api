package com.assetdock.api.audit.domain;

public interface AuditLogRepository {

	void save(AuditLogEntry entry);
}
