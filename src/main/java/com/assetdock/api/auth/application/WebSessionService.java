package com.assetdock.api.auth.application;

import com.assetdock.api.audit.application.AuditLogCommand;
import com.assetdock.api.audit.application.AuditLogService;
import com.assetdock.api.audit.domain.AuditEventType;
import com.assetdock.api.auth.domain.WebSession;
import com.assetdock.api.auth.domain.WebSessionRepository;
import com.assetdock.api.auth.infrastructure.WebSessionProperties;
import com.assetdock.api.security.auth.AuthenticatedUserPrincipal;
import com.assetdock.api.user.domain.User;
import com.assetdock.api.user.domain.UserRepository;
import com.assetdock.api.user.domain.UserStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WebSessionService {

	private final AuthenticationService authenticationService;
	private final WebSessionRepository webSessionRepository;
	private final UserRepository userRepository;
	private final AuditLogService auditLogService;
	private final WebSessionProperties webSessionProperties;
	private final Clock clock;

	public WebSessionService(
		AuthenticationService authenticationService,
		WebSessionRepository webSessionRepository,
		UserRepository userRepository,
		AuditLogService auditLogService,
		WebSessionProperties webSessionProperties,
		Clock clock
	) {
		this.authenticationService = authenticationService;
		this.webSessionRepository = webSessionRepository;
		this.userRepository = userRepository;
		this.auditLogService = auditLogService;
		this.webSessionProperties = webSessionProperties;
		this.clock = clock;
	}

	@Transactional
	public WebAuthenticatedSession create(LoginCommand command) {
		AuthenticatedLogin authenticatedLogin = authenticationService.authenticateForWeb(command);
		Instant now = authenticatedLogin.authenticatedAt();
		WebSession session = new WebSession(
			UUID.randomUUID(),
			authenticatedLogin.user().id(),
			UUID.randomUUID().toString(),
			now,
			now,
			now.plus(webSessionProperties.absoluteLifetime()),
			null
		);
		webSessionRepository.save(session);
		recordSessionAudit(AuditEventType.WEB_SESSION_CREATED, authenticatedLogin.user(), session, "SUCCESS");
		return new WebAuthenticatedSession(session, authenticatedLogin.user(), authenticatedLogin.principal());
	}

	@Transactional
	public Optional<WebAuthenticatedSession> resolve(String rawSessionId) {
		UUID sessionId = parseSessionId(rawSessionId);
		if (sessionId == null) {
			return Optional.empty();
		}

		Optional<WebSession> sessionOptional = webSessionRepository.findById(sessionId);
		if (sessionOptional.isEmpty()) {
			return Optional.empty();
		}

		WebSession session = sessionOptional.get();
		if (session.invalidatedAt() != null) {
			return Optional.empty();
		}

		Instant now = Instant.now(clock);
		if (isExpired(session, now)) {
			invalidateExpired(session, now);
			return Optional.empty();
		}

		Optional<User> userOptional = userRepository.findById(session.userId());
		if (userOptional.isEmpty()) {
			webSessionRepository.invalidate(session.id(), now);
			return Optional.empty();
		}

		User user = userOptional.get();
		if (user.status() != UserStatus.ACTIVE) {
			webSessionRepository.invalidate(session.id(), now);
			return Optional.empty();
		}

		webSessionRepository.updateLastActiveAt(session.id(), now);
		return Optional.of(new WebAuthenticatedSession(
			new WebSession(
				session.id(),
				session.userId(),
				session.csrfToken(),
				session.createdAt(),
				now,
				session.expiresAt(),
				session.invalidatedAt()
			),
			user,
			AuthenticatedUserPrincipal.from(user)
		));
	}

	@Transactional
	public void logout(UUID sessionId) {
		Optional<WebSession> sessionOptional = webSessionRepository.findById(sessionId);
		if (sessionOptional.isEmpty()) {
			return;
		}

		WebSession session = sessionOptional.get();
		if (session.invalidatedAt() != null) {
			return;
		}

		Instant now = Instant.now(clock);
		webSessionRepository.invalidate(session.id(), now);
		userRepository.findById(session.userId()).ifPresent(user ->
			recordSessionAudit(AuditEventType.WEB_SESSION_LOGGED_OUT, user, session, "SUCCESS")
		);
	}

	private void invalidateExpired(WebSession session, Instant now) {
		webSessionRepository.invalidate(session.id(), now);
		userRepository.findById(session.userId()).ifPresent(user ->
			recordSessionAudit(AuditEventType.WEB_SESSION_EXPIRED, user, session, "SUCCESS")
		);
	}

	private boolean isExpired(WebSession session, Instant now) {
		return now.isAfter(session.expiresAt())
			|| now.isAfter(session.lastActiveAt().plus(webSessionProperties.idleTimeout()));
	}

	private UUID parseSessionId(String rawSessionId) {
		if (rawSessionId == null || rawSessionId.isBlank()) {
			return null;
		}

		try {
			return UUID.fromString(rawSessionId);
		}
		catch (IllegalArgumentException exception) {
			return null;
		}
	}

	private void recordSessionAudit(
		AuditEventType eventType,
		User user,
		WebSession session,
		String outcome
	) {
		Map<String, Object> details = new LinkedHashMap<>();
		details.put("authMode", "web_session");
		details.put("sessionIdPrefix", session.id().toString().substring(0, 8));
		details.put("expiresAt", session.expiresAt().toString());

		auditLogService.record(new AuditLogCommand(
			user.organizationId(),
			user.id(),
			eventType,
			"web_session",
			null,
			outcome,
			details
		));
	}
}
