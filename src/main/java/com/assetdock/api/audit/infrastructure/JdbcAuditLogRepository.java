package com.assetdock.api.audit.infrastructure;

import com.assetdock.api.audit.domain.AuditLogEntry;
import com.assetdock.api.audit.domain.AuditLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAuditLogRepository implements AuditLogRepository {

	private final JdbcClient jdbcClient;
	private final ObjectMapper objectMapper;

	public JdbcAuditLogRepository(JdbcClient jdbcClient, ObjectMapper objectMapper) {
		this.jdbcClient = jdbcClient;
		this.objectMapper = objectMapper;
	}

	@Override
	public void save(AuditLogEntry entry) {
		jdbcClient.sql("""
			INSERT INTO audit_logs (
				id,
				organization_id,
				actor_user_id,
				event_type,
				resource_type,
				resource_id,
				outcome,
				ip_address,
				user_agent,
				request_id,
				details_json,
				occurred_at
			)
			VALUES (
				:id,
				:organizationId,
				:actorUserId,
				:eventType,
				:resourceType,
				:resourceId,
				:outcome,
				:ipAddress,
				:userAgent,
				:requestId,
				CAST(:detailsJson AS jsonb),
				:occurredAt
			)
			""")
			.param("id", entry.id())
			.param("organizationId", entry.organizationId())
			.param("actorUserId", entry.actorUserId())
			.param("eventType", entry.eventType().name())
			.param("resourceType", entry.resourceType())
			.param("resourceId", entry.resourceId())
			.param("outcome", entry.outcome())
			.param("ipAddress", entry.ipAddress())
			.param("userAgent", entry.userAgent())
			.param("requestId", entry.requestId())
			.param("detailsJson", serialize(entry))
			.param("occurredAt", entry.occurredAt())
			.update();
	}

	private String serialize(AuditLogEntry entry) {
		try {
			return objectMapper.writeValueAsString(entry.details());
		}
		catch (JsonProcessingException exception) {
			throw new IllegalStateException("Could not serialize audit log details.", exception);
		}
	}
}
