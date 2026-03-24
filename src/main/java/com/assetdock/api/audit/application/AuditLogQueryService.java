package com.assetdock.api.audit.application;

import com.assetdock.api.audit.domain.AuditEventType;
import com.assetdock.api.audit.domain.AuditLogEntry;
import com.assetdock.api.audit.domain.AuditLogRepository;
import com.assetdock.api.security.auth.AuthenticatedUserPrincipal;
import com.assetdock.api.security.auth.TenantAccessService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogQueryService {

	private static final int DEFAULT_PAGE = 0;
	private static final int DEFAULT_SIZE = 20;
	private static final int MAX_SIZE = 100;

	private final AuditLogRepository auditLogRepository;
	private final TenantAccessService tenantAccessService;

	public AuditLogQueryService(
		AuditLogRepository auditLogRepository,
		TenantAccessService tenantAccessService
	) {
		this.auditLogRepository = auditLogRepository;
		this.tenantAccessService = tenantAccessService;
	}

	@Transactional(readOnly = true)
	public AuditLogPageView list(
		AuthenticatedUserPrincipal actor,
		Integer page,
		Integer size,
		AuditEventType eventType,
		Instant from,
		Instant to,
		UUID organizationId
	) {
		int resolvedPage = page == null ? DEFAULT_PAGE : page;
		int resolvedSize = size == null ? DEFAULT_SIZE : size;
		validatePagination(resolvedPage, resolvedSize);
		validateDateRange(from, to);

		UUID scopeOrganizationId = resolveScopeOrganizationId(actor, organizationId);
		long totalElements = auditLogRepository.countByCriteria(scopeOrganizationId, eventType, from, to);
		int offset = resolvedPage * resolvedSize;
		List<AuditLogView> items = auditLogRepository.findByCriteria(
			scopeOrganizationId,
			eventType,
			from,
			to,
			resolvedSize,
			offset
		).stream()
			.map(this::toView)
			.toList();

		int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / resolvedSize);
		return new AuditLogPageView(
			items,
			resolvedPage,
			resolvedSize,
			totalElements,
			totalPages
		);
	}

	private UUID resolveScopeOrganizationId(AuthenticatedUserPrincipal actor, UUID organizationId) {
		if (actor.isSuperAdmin()) {
			return organizationId;
		}

		UUID actorOrganizationId = requireActorOrganizationId(actor);
		UUID targetOrganizationId = organizationId == null ? actorOrganizationId : organizationId;
		tenantAccessService.requireAuditLogReadAccess(actor, targetOrganizationId);
		return targetOrganizationId;
	}

	private AuditLogView toView(AuditLogEntry entry) {
		return new AuditLogView(
			entry.id(),
			entry.organizationId(),
			entry.actorUserId(),
			entry.eventType(),
			entry.resourceType(),
			entry.resourceId(),
			entry.outcome(),
			entry.requestId(),
			entry.details(),
			entry.occurredAt()
		);
	}

	private UUID requireActorOrganizationId(AuthenticatedUserPrincipal actor) {
		if (actor.organizationId() == null) {
			throw new InvalidAuditLogQueryException("Tenant organization context is required for audit log queries.");
		}
		return actor.organizationId();
	}

	private void validatePagination(int page, int size) {
		if (page < 0) {
			throw new InvalidAuditLogQueryException("page must be greater than or equal to 0.");
		}
		if (size <= 0 || size > MAX_SIZE) {
			throw new InvalidAuditLogQueryException("size must be between 1 and 100.");
		}
	}

	private void validateDateRange(Instant from, Instant to) {
		if (from != null && to != null && from.isAfter(to)) {
			throw new InvalidAuditLogQueryException("from must be less than or equal to to.");
		}
	}
}
