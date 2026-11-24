package com.assetdock.api.asset.application;

import com.assetdock.api.asset.domain.Asset;
import com.assetdock.api.asset.domain.AssetRepository;
import com.assetdock.api.audit.domain.AuditLogEntry;
import com.assetdock.api.audit.domain.AuditLogRepository;
import com.assetdock.api.security.auth.AuthenticatedUserPrincipal;
import com.assetdock.api.security.auth.TenantAccessService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AssetTimelineService {

    private final AuditLogRepository auditLogRepository;
    private final AssetRepository assetRepository;
    private final TenantAccessService tenantAccessService;

    public AssetTimelineService(
        AuditLogRepository auditLogRepository,
        AssetRepository assetRepository,
        TenantAccessService tenantAccessService
    ) {
        this.auditLogRepository = auditLogRepository;
        this.assetRepository = assetRepository;
        this.tenantAccessService = tenantAccessService;
    }

    @Transactional(readOnly = true)
    public List<TimelineEventView> getAssetTimeline(AuthenticatedUserPrincipal principal, UUID assetId) {
        Asset asset;
        if (principal.isSuperAdmin()) {
            asset = assetRepository.findById(assetId).orElseThrow(AssetNotFoundException::new);
        } else {
            UUID organizationId = requireActorOrganizationId(principal);
            tenantAccessService.requireAssetReadAccess(principal, organizationId);
            asset = assetRepository.findByIdAndOrganizationId(assetId, organizationId)
                .orElseThrow(AssetNotFoundException::new);
        }

        List<AuditLogEntry> logs = auditLogRepository.findByResourceId(asset.organizationId(), asset.id());

        return logs.stream()
            .map(log -> new TimelineEventView(
                log.id(),
                log.eventType(),
                log.actorUserId(),
                log.occurredAt(),
                log.details()
            ))
            .collect(Collectors.toList());
    }

    private UUID requireActorOrganizationId(AuthenticatedUserPrincipal actor) {
        if (actor.organizationId() == null) {
            throw new InvalidAssetRequestException("Tenant organization context is required for asset operations.");
        }
        return actor.organizationId();
    }
}
