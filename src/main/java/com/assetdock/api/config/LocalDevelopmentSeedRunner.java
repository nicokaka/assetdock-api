package com.assetdock.api.config;

import com.assetdock.api.organization.domain.Organization;
import com.assetdock.api.organization.domain.OrganizationRepository;
import com.assetdock.api.user.application.InvalidUserRequestException;
import com.assetdock.api.user.domain.User;
import com.assetdock.api.user.domain.UserRepository;
import com.assetdock.api.user.domain.UserRole;
import com.assetdock.api.user.domain.UserStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("local")
public class LocalDevelopmentSeedRunner implements ApplicationRunner {

	private static final Logger LOGGER = LoggerFactory.getLogger(LocalDevelopmentSeedRunner.class);

	private final LocalSeedProperties properties;
	private final OrganizationRepository organizationRepository;
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final Clock clock;

	public LocalDevelopmentSeedRunner(
		LocalSeedProperties properties,
		OrganizationRepository organizationRepository,
		UserRepository userRepository,
		PasswordEncoder passwordEncoder,
		Clock clock
	) {
		this.properties = properties;
		this.organizationRepository = organizationRepository;
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.clock = clock;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (!properties.enabled()) {
			LOGGER.info("local_seed status=disabled");
			return;
		}

		validateProperties();

		String normalizedEmail = normalizeEmail(properties.adminEmail());
		Organization organization = organizationRepository.findBySlug(properties.organizationSlug())
			.orElseGet(this::createOrganization);

		if (userRepository.existsByEmail(normalizedEmail)) {
			LOGGER.info("local_seed status=skipped reason=user_exists email={}", normalizedEmail);
			return;
		}

		Instant now = Instant.now(clock);
		User user = new User(
			UUID.randomUUID(),
			organization.id(),
			normalizedEmail,
			properties.adminFullName().trim(),
			passwordEncoder.encode(properties.adminPassword()),
			UserStatus.ACTIVE,
			Set.of(UserRole.ORG_ADMIN),
			0,
			null,
			now,
			now
		);
		userRepository.save(user);

		LOGGER.info(
			"local_seed status=completed organization_id={} admin_user_id={}",
			organization.id(),
			user.id()
		);
	}

	private Organization createOrganization() {
		Instant now = Instant.now(clock);
		Organization organization = new Organization(
			UUID.randomUUID(),
			properties.organizationName().trim(),
			properties.organizationSlug().trim(),
			now,
			now
		);
		organizationRepository.save(organization);
		LOGGER.info("local_seed status=organization_created organization_id={}", organization.id());
		return organization;
	}

	private void validateProperties() {
		if (isBlank(properties.organizationName())) {
			throw new InvalidUserRequestException("LOCAL_SEED_ORG_NAME is required when local seed is enabled.");
		}
		if (isBlank(properties.organizationSlug())) {
			throw new InvalidUserRequestException("LOCAL_SEED_ORG_SLUG is required when local seed is enabled.");
		}
		if (isBlank(properties.adminFullName())) {
			throw new InvalidUserRequestException("LOCAL_SEED_ADMIN_FULL_NAME is required when local seed is enabled.");
		}
		if (isBlank(properties.adminEmail())) {
			throw new InvalidUserRequestException("LOCAL_SEED_ADMIN_EMAIL is required when local seed is enabled.");
		}
		if (isBlank(properties.adminPassword())) {
			throw new InvalidUserRequestException("LOCAL_SEED_ADMIN_PASSWORD is required when local seed is enabled.");
		}
	}

	private String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
