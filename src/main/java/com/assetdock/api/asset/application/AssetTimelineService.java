package com.assetdock.api.asset.application;

import com.assetdock.api.asset.domain.AssetRepository;
import com.assetdock.api.audit.domain.AuditLogEntry;
import com.assetdock.api.audit.domain.AuditLogRepository;
import com.assetdock.api.security.auth.AuthenticatedUserPrincipal;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AssetTimelineService {

    private final AuditLogRepository auditLogRepository;
    private final AssetRepository assetRepository;

    public AssetTimelineService(AuditLogRepository auditLogRepository, AssetRepository assetRepository) {
        this.auditLogRepository = auditLogRepository;
        this.assetRepository = assetRepository;
    }

    public List<TimelineEventView> getAssetTimeline(AuthenticatedUserPrincipal principal, UUID assetId) {
        // Verify user has access to the asset
        assetRepository.findByIdAndOrganizationId(assetId, principal.organizationId())
            .orElseThrow(AssetNotFoundException::new);

        List<AuditLogEntry> logs = auditLogRepository.findByResourceId(principal.organizationId(), assetId);

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
}
