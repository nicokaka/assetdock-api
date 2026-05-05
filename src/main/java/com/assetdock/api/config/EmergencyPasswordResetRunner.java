package com.assetdock.api.config;

import com.assetdock.api.audit.application.AuditLogCommand;
import com.assetdock.api.audit.application.AuditLogService;
import com.assetdock.api.audit.domain.AuditEventType;
import com.assetdock.api.auth.domain.WebSessionRepository;
import com.assetdock.api.common.util.EmailNormalizer;
import com.assetdock.api.user.domain.User;
import com.assetdock.api.user.domain.UserRepository;
import com.assetdock.api.user.domain.UserStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class EmergencyPasswordResetRunner implements ApplicationRunner {

	private static final Logger LOGGER = LoggerFactory.getLogger(EmergencyPasswordResetRunner.class);

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final WebSessionRepository webSessionRepository;
	private final AuditLogService auditLogService;
	private final ApplicationContext applicationContext;
	private final TransactionTemplate transactionTemplate;
	private final Clock clock;

	public EmergencyPasswordResetRunner(
		UserRepository userRepository,
		PasswordEncoder passwordEncoder,
		WebSessionRepository webSessionRepository,
		AuditLogService auditLogService,
		ApplicationContext applicationContext,
		TransactionTemplate transactionTemplate,
		Clock clock
	) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.webSessionRepository = webSessionRepository;
		this.auditLogService = auditLogService;
		this.applicationContext = applicationContext;
		this.transactionTemplate = transactionTemplate;
		this.clock = clock;
	}

	@Override
	public void run(ApplicationArguments args) {
		String email = System.getenv("EMERGENCY_RESET_EMAIL");
		String newPassword = System.getenv("EMERGENCY_RESET_PASSWORD");

		if (email == null || newPassword == null || email.isBlank() || newPassword.isBlank()) {
			return;
		}

		LOGGER.info("Emergency password reset requested for email: {}", email);

		String normalizedEmail = EmailNormalizer.normalize(email);
		User user = userRepository.findByEmail(normalizedEmail).orElse(null);

		if (user == null) {
			LOGGER.error("Emergency password reset failed: User not found with email {}", normalizedEmail);
			exitWithCode(1);
			return;
		}

		if (newPassword.length() < 8) {
			LOGGER.error("Emergency password reset failed: New password must be at least 8 characters long.");
			exitWithCode(1);
			return;
		}

		// Execute all DB operations inside a dedicated transaction that commits before exit.
		Instant now = Instant.now(clock);
		String newHash = passwordEncoder.encode(newPassword);
		transactionTemplate.execute(status -> {
			userRepository.updatePasswordHash(user.id(), newHash, now);

			if (user.status() == UserStatus.LOCKED) {
				userRepository.updateStatus(user.id(), UserStatus.ACTIVE, now);
				userRepository.resetFailedLoginAttempts(user.id(), now);
				LOGGER.info("User account unlocked during emergency reset.");
			}

			webSessionRepository.invalidateAllByUserId(user.id(), now);

			auditLogService.record(new AuditLogCommand(
				user.organizationId(),
				user.id(),
				AuditEventType.PASSWORD_RESET_BY_ADMIN,
				"user",
				user.id(),
				"SUCCESS",
				Map.of(
					"source", "cli_emergency",
					"sessionsRevoked", "true",
					"accountUnlocked", String.valueOf(user.status() == UserStatus.LOCKED)
				)
			));
			return null;
		});

		LOGGER.info("Emergency password reset completed successfully for user id: {}. Shutting down.", user.id());
		exitWithCode(0);
	}

	private void exitWithCode(int code) {
		SpringApplication.exit(applicationContext, () -> code);
		System.exit(code);
	}
}
