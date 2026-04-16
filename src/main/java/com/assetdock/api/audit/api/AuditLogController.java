package com.assetdock.api.audit.api;

import com.assetdock.api.audit.application.AuditLogPageView;
import com.assetdock.api.audit.application.AuditLogQueryService;
import com.assetdock.api.audit.domain.AuditEventType;
import com.assetdock.api.security.auth.AuthenticatedUserPrincipal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/audit-logs")
public class AuditLogController {

	private final AuditLogQueryService auditLogQueryService;

	public AuditLogController(AuditLogQueryService auditLogQueryService) {
		this.auditLogQueryService = auditLogQueryService;
	}

	@GetMapping
	AuditLogPageView list(
		@AuthenticationPrincipal AuthenticatedUserPrincipal principal,
		@RequestParam(required = false) Integer page,
		@RequestParam(required = false) Integer size,
		@RequestParam(required = false) AuditEventType eventType,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
		@RequestParam(required = false) UUID organizationId
	) {
		return auditLogQueryService.list(principal, page, size, eventType, from, to, organizationId);
	}
}
