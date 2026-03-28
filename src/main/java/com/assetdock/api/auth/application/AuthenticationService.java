package com.assetdock.api.auth.application;

import com.assetdock.api.audit.application.AuditLogCommand;
import com.assetdock.api.audit.application.AuditLogService;
import com.assetdock.api.audit.domain.AuditEventType;
import com.assetdock.api.auth.infrastructure.JwtTokenService;
import com.assetdock.api.security.auth.AuthenticatedUserPrincipal;
import com.assetdock.api.user.domain.User;
import com.assetdock.api.user.domain.UserRepository;
import com.assetdock.api.user.domain.UserStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenService jwtTokenService;
	private final AuditLogService auditLogService;
	private final AuthHardeningProperties authHardeningProperties;
	private final Clock clock;

	public AuthenticationService(
		UserRepository userRepository,
		PasswordEncoder passwordEncoder,
		JwtTokenService jwtTokenService,
		AuditLogService auditLogService,
		AuthHardeningProperties authHardeningProperties,
		Clock clock
	) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtTokenService = jwtTokenService;
		this.auditLogService = auditLogService;
		this.authHardeningProperties = authHardeningProperties;
		this.clock = clock;
	}

	@Transactional
	public LoginResult login(LoginCommand command) {
		String normalizedEmail = normalizeEmail(command.email());
		User user = userRepository.findByEmail(normalizedEmail)
			.orElseThrow(() -> {
				recordLoginFailure(null, null, null, normalizedEmail, "user_not_found");
				return new InvalidCredentialsException();
			});

		if (!passwordEncoder.matches(command.password(), user.passwordHash())) {
			handleFailedPasswordAttempt(user, normalizedEmail);
			recordLoginFailure(user.organizationId(), null, user.id(), normalizedEmail, "invalid_password");
			throw new InvalidCredentialsException();
		}

		if (user.status() == UserStatus.INACTIVE) {
			recordLoginFailure(user.organizationId(), null, user.id(), normalizedEmail, "inactive");
			throw new InactiveUserAuthenticationException();
		}

		if (user.status() == UserStatus.LOCKED) {
			recordLoginFailure(user.organizationId(), null, user.id(), normalizedEmail, "locked");
			throw new LockedUserAuthenticationException();
		}

		Instant loginAt = Instant.now(clock);
		int previousFailedLoginAttempts = user.failedLoginAttempts();
		User authenticatedUser = user;
		if (user.failedLoginAttempts() > 0) {
			authenticatedUser = userRepository.resetFailedLoginAttempts(user.id(), loginAt);
		}
		userRepository.updateLastLoginAt(user.id(), loginAt);

		AuthenticatedUserPrincipal principal = AuthenticatedUserPrincipal.from(authenticatedUser);
		JwtTokenService.IssuedToken issuedToken = jwtTokenService.issue(principal, loginAt);
		auditLogService.record(new AuditLogCommand(
			authenticatedUser.organizationId(),
			authenticatedUser.id(),
			AuditEventType.LOGIN_SUCCESS,
			"user",
			authenticatedUser.id(),
			"SUCCESS",
			loginSuccessDetails(authenticatedUser, previousFailedLoginAttempts)
		));

		return new LoginResult(issuedToken.value(), issuedToken.expiresInSeconds(), principal);
	}

	private void handleFailedPasswordAttempt(User user, String normalizedEmail) {
		if (user.status() != UserStatus.ACTIVE) {
			return;
		}

		Instant now = Instant.now(clock);
		User updatedUser = userRepository.incrementFailedLoginAttempts(user.id(), now);
		if (updatedUser.failedLoginAttempts() < authHardeningProperties.maxFailedLoginAttempts()) {
			return;
		}

		User lockedUser = userRepository.updateStatus(user.id(), UserStatus.LOCKED, now);
		auditLogService.record(new AuditLogCommand(
			lockedUser.organizationId(),
			null,
			AuditEventType.USER_LOCKED,
			"user",
			lockedUser.id(),
			"SUCCESS",
			java.util.Map.of(
				"email", normalizedEmail,
				"reason", "repeated_failed_logins",
				"failedLoginAttempts", lockedUser.failedLoginAttempts(),
				"threshold", authHardeningProperties.maxFailedLoginAttempts()
			)
		));
	}

	private java.util.Map<String, Object> loginSuccessDetails(User user, int previousFailedLoginAttempts) {
		java.util.Map<String, Object> details = new java.util.LinkedHashMap<>();
		details.put("email", user.email());
		details.put("roles", user.roles().stream().map(Enum::name).toList());
		if (previousFailedLoginAttempts == 0) {
			return details;
		}

		details.put("failedLoginAttemptsReset", true);
		details.put("previousFailedLoginAttempts", previousFailedLoginAttempts);
		return details;
	}

	private String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

	private void recordLoginFailure(
		java.util.UUID organizationId,
		java.util.UUID actorUserId,
		java.util.UUID resourceId,
		String email,
		String reason
	) {
		auditLogService.record(new AuditLogCommand(
			organizationId,
			actorUserId,
			AuditEventType.LOGIN_FAILURE,
			"user",
			resourceId,
			"FAILURE",
			java.util.Map.of(
				"email", email,
				"reason", reason
			)
		));
	}
}
