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
	private final Clock clock;

	public AuthenticationService(
		UserRepository userRepository,
		PasswordEncoder passwordEncoder,
		JwtTokenService jwtTokenService,
		AuditLogService auditLogService,
		Clock clock
	) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtTokenService = jwtTokenService;
		this.auditLogService = auditLogService;
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
		userRepository.updateLastLoginAt(user.id(), loginAt);

		AuthenticatedUserPrincipal principal = AuthenticatedUserPrincipal.from(user);
		JwtTokenService.IssuedToken issuedToken = jwtTokenService.issue(principal, loginAt);
		auditLogService.record(new AuditLogCommand(
			user.organizationId(),
			user.id(),
			AuditEventType.LOGIN_SUCCESS,
			"user",
			user.id(),
			"SUCCESS",
			java.util.Map.of(
				"email", user.email(),
				"roles", user.roles().stream().map(Enum::name).toList()
			)
		));

		return new LoginResult(issuedToken.value(), issuedToken.expiresInSeconds(), principal);
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
