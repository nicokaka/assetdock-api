package com.assetdock.api.audit.application;

import com.assetdock.api.audit.domain.AuditLogRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "app.cleanup.enabled", havingValue = "true", matchIfMissing = true)
public class AuditLogCleanupTask {

	private static final Logger LOGGER = LoggerFactory.getLogger(AuditLogCleanupTask.class);
	private static final int RETENTION_DAYS = 730; // 2 years

	private final AuditLogRepository auditLogRepository;
	private final Clock clock;

	public AuditLogCleanupTask(AuditLogRepository auditLogRepository, Clock clock) {
		this.auditLogRepository = auditLogRepository;
		this.clock = clock;
	}

	@Scheduled(cron = "0 0 2 * * *") // Every day at 2 AM
	@Transactional
	public void cleanupExpiredAuditLogs() {
		Instant cutoff = Instant.now(clock).minus(RETENTION_DAYS, ChronoUnit.DAYS);
		try {
			int deletedCount = auditLogRepository.deleteOlderThan(cutoff);
			LOGGER.info("Cleaned up {} expired audit logs older than {}", deletedCount, cutoff);
		} catch (Exception e) {
			LOGGER.error("Failed to clean up expired audit logs", e);
		}
	}
}
