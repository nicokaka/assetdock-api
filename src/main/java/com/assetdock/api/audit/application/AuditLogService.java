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
}
