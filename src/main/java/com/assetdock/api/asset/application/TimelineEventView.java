package com.assetdock.api.asset.application;

import com.assetdock.api.audit.domain.AuditEventType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record TimelineEventView(
    UUID id,
    AuditEventType eventType,
    UUID actorUserId,
    Instant occurredAt,
    Map<String, Object> details
) {
}
