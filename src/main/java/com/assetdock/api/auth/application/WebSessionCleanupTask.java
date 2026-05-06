package com.assetdock.api.auth.application;

import com.assetdock.api.auth.domain.WebSessionRepository;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "app.cleanup.enabled", havingValue = "true", matchIfMissing = true)
public class WebSessionCleanupTask {

	private static final Logger LOGGER = LoggerFactory.getLogger(WebSessionCleanupTask.class);

	private final WebSessionRepository webSessionRepository;
	private final Clock clock;

	public WebSessionCleanupTask(WebSessionRepository webSessionRepository, Clock clock) {
		this.webSessionRepository = webSessionRepository;
		this.clock = clock;
	}

	@Scheduled(cron = "0 0 * * * *") // Every hour
	@Transactional
	public void cleanupExpiredSessions() {
		Instant now = Instant.now(clock);
		try {
			webSessionRepository.deleteExpiredBefore(now);
			LOGGER.info("Cleaned up expired web sessions at {}", now);
		} catch (Exception e) {
			LOGGER.error("Failed to clean up expired web sessions", e);
		}
	}
}
