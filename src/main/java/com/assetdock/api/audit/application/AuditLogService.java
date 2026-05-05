package com.assetdock.api.audit.application;

import com.assetdock.api.audit.domain.AuditLogEntry;
import com.assetdock.api.audit.domain.AuditLogRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

	private final AuditLogRepository auditLogRepository;
	private final AuditContextProvider auditContextProvider;
	private final Clock clock;

	public AuditLogService(
		AuditLogRepository auditLogRepository,
		AuditContextProvider auditContextProvider,
		Clock clock
	) {
		this.auditLogRepository = auditLogRepository;
		this.auditContextProvider = auditContextProvider;
		this.clock = clock;
	}

	/**
	 * Records an audit entry in a NEW, independent transaction.
	 * Use this when the audit must succeed even if the caller's transaction is rolled back.
	 * WARNING: do NOT call this before the referenced entities are committed — it will violate FKs.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void record(AuditLogCommand command) {
		AuditContext context = auditContextProvider.current();
		AuditLogEntry entry = new AuditLogEntry(
			UUID.randomUUID(),
			command.organizationId(),
			command.actorUserId(),
			command.eventType(),
			command.resourceType(),
			command.resourceId(),
			command.outcome(),
			context.ipAddress(),
			context.userAgent(),
			context.requestId(),
			command.details() == null ? Map.of() : Map.copyOf(command.details()),
			Instant.now(clock)
		);

		auditLogRepository.save(entry);
	}

	/**
	 * Records an audit entry within the caller's existing transaction.
	 * Use this when the audited entities are not yet committed and REQUIRES_NEW would cause FK violations.
	 * The audit entry will be rolled back together with the caller's transaction if it fails.
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public void recordInCurrentTransaction(AuditLogCommand command) {
		AuditContext context = auditContextProvider.current();
		AuditLogEntry entry = new AuditLogEntry(
			UUID.randomUUID(),
			command.organizationId(),
			command.actorUserId(),
			command.eventType(),
			command.resourceType(),
			command.resourceId(),
			command.outcome(),
			context.ipAddress(),
			context.userAgent(),
			context.requestId(),
			command.details() == null ? Map.of() : Map.copyOf(command.details()),
			Instant.now(clock)
		);

		auditLogRepository.save(entry);
	}
}
