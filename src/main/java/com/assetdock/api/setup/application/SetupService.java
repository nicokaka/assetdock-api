package com.assetdock.api.setup.application;

import com.assetdock.api.audit.application.AuditLogCommand;
import com.assetdock.api.audit.application.AuditLogService;
import com.assetdock.api.audit.domain.AuditEventType;
import com.assetdock.api.common.util.EmailNormalizer;
import com.assetdock.api.organization.domain.Organization;
import com.assetdock.api.organization.domain.OrganizationRepository;
import com.assetdock.api.user.domain.User;
import com.assetdock.api.user.domain.UserRepository;
import com.assetdock.api.user.domain.UserRole;
import com.assetdock.api.user.domain.UserStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SetupService {

	private static final Logger LOGGER = LoggerFactory.getLogger(SetupService.class);

	private final OrganizationRepository organizationRepository;
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuditLogService auditLogService;
	private final JdbcClient jdbcClient;
	private final Clock clock;

	public SetupService(
		OrganizationRepository organizationRepository,
		UserRepository userRepository,
		PasswordEncoder passwordEncoder,
		AuditLogService auditLogService,
		JdbcClient jdbcClient,
		Clock clock
	) {
		this.organizationRepository = organizationRepository;
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.auditLogService = auditLogService;
		this.jdbcClient = jdbcClient;
		this.clock = clock;
	}

	public boolean isConfigured() {
		Long count = jdbcClient.sql("SELECT COUNT(*) FROM organizations")
			.query(Long.class)
			.single();
		return count != null && count > 0;
	}

	@Transactional
	public SetupResult setup(SetupCommand command) {
		if (isConfigured()) {
			throw new SystemAlreadyConfiguredException();
		}

		// Use a Postgres transaction-level advisory lock to guarantee mutual exclusion.
		// hashtext('assetdock_setup_lock') generates a deterministic integer lock ID.
		jdbcClient.sql("SELECT pg_advisory_xact_lock(hashtext('assetdock_setup_lock'))")
			.query()
			.singleRow();

		if (isConfigured()) {
			throw new SystemAlreadyConfiguredException();
		}

		Instant now = Instant.now(clock);

		String slug = generateSlug(command.organizationName());
		Organization organization = new Organization(
			UUID.randomUUID(),
			command.organizationName().trim(),
			slug,
			now,
			now
		);
		organizationRepository.save(organization);

		String normalizedEmail = EmailNormalizer.normalize(command.adminEmail());
		User admin = new User(
			UUID.randomUUID(),
			organization.id(),
			normalizedEmail,
			command.adminFullName().trim(),
			passwordEncoder.encode(command.adminPassword()),
			UserStatus.ACTIVE,
			Set.of(UserRole.ORG_ADMIN),
			0,
			null,
			now,
			now
		);
		userRepository.save(admin);

		auditLogService.record(new AuditLogCommand(
			organization.id(),
			admin.id(),
			AuditEventType.SYSTEM_SETUP_COMPLETED,
			"organization",
			organization.id(),
			"SUCCESS",
			Map.of(
				"organizationName", organization.name(),
				"adminEmail", admin.email()
			)
		));

		LOGGER.info(
			"system_setup status=completed organization_id={} admin_user_id={}",
			organization.id(),
			admin.id()
		);

		return new SetupResult(organization, admin);
	}

	private String generateSlug(String organizationName) {
		return organizationName.trim()
			.toLowerCase(Locale.ROOT)
			.replaceAll("[^a-z0-9\\s-]", "")
			.replaceAll("[\\s]+", "-")
			.replaceAll("-+", "-")
			.replaceAll("^-|-$", "");
	}
}
