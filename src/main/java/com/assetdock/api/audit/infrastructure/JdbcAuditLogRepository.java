package com.assetdock.api.audit.infrastructure;

import com.assetdock.api.audit.domain.AuditLogEntry;
import com.assetdock.api.audit.domain.AuditEventType;
import com.assetdock.api.audit.domain.AuditLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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

	@Override
	public long countByCriteria(
		UUID organizationId,
		AuditEventType eventType,
		Instant from,
		Instant to
	) {
		JdbcClient.StatementSpec statement = jdbcClient.sql("SELECT COUNT(*) " + buildWhereClause(organizationId, eventType, from, to));
		statement = bindCriteria(statement, organizationId, eventType, from, to);
		return statement.query(Long.class).single();
	}

	@Override
	public List<AuditLogEntry> findByCriteria(
		UUID organizationId,
		AuditEventType eventType,
		Instant from,
		Instant to,
		int limit,
		int offset
	) {
		String sql = """
			SELECT id, organization_id, actor_user_id, event_type, resource_type, resource_id, outcome,
			       ip_address, user_agent, request_id, details_json::text AS details_json, occurred_at
			""" + buildWhereClause(organizationId, eventType, from, to) + """
			ORDER BY occurred_at DESC
			LIMIT :limit OFFSET :offset
			""";

		JdbcClient.StatementSpec statement = jdbcClient.sql(sql)
			.param("limit", limit)
			.param("offset", offset);
		statement = bindCriteria(statement, organizationId, eventType, from, to);
		return statement.query(this::mapAuditLogEntry).list();
	}

	private String buildWhereClause(
		UUID organizationId,
		AuditEventType eventType,
		Instant from,
		Instant to
	) {
		StringBuilder sqlBuilder = new StringBuilder("FROM audit_logs WHERE 1=1");
		if (organizationId != null) {
			sqlBuilder.append(" AND organization_id = :organizationId");
		}
		if (eventType != null) {
			sqlBuilder.append(" AND event_type = :eventType");
		}
		if (from != null) {
			sqlBuilder.append(" AND occurred_at >= :from");
		}
		if (to != null) {
			sqlBuilder.append(" AND occurred_at <= :to");
		}
		return sqlBuilder.toString();
	}

	private JdbcClient.StatementSpec bindCriteria(
		JdbcClient.StatementSpec statement,
		UUID organizationId,
		AuditEventType eventType,
		Instant from,
		Instant to
	) {
		JdbcClient.StatementSpec result = statement;
		if (organizationId != null) {
			result = result.param("organizationId", organizationId);
		}
		if (eventType != null) {
			result = result.param("eventType", eventType.name());
		}
		if (from != null) {
			result = result.param("from", from);
		}
		if (to != null) {
			result = result.param("to", to);
		}
		return result;
	}

	private AuditLogEntry mapAuditLogEntry(ResultSet resultSet, int rowNum) throws SQLException {
		return new AuditLogEntry(
			resultSet.getObject("id", UUID.class),
			resultSet.getObject("organization_id", UUID.class),
			resultSet.getObject("actor_user_id", UUID.class),
			AuditEventType.valueOf(resultSet.getString("event_type")),
			resultSet.getString("resource_type"),
			resultSet.getObject("resource_id", UUID.class),
			resultSet.getString("outcome"),
			resultSet.getString("ip_address"),
			resultSet.getString("user_agent"),
			resultSet.getString("request_id"),
			deserializeDetails(resultSet.getString("details_json")),
			resultSet.getObject("occurred_at", Instant.class)
		);
	}

	private Map<String, Object> deserializeDetails(String detailsJson) {
		if (detailsJson == null || detailsJson.isBlank()) {
			return Map.of();
		}

		try {
			return objectMapper.readValue(detailsJson, new TypeReference<LinkedHashMap<String, Object>>() {
			});
		}
		catch (JsonProcessingException exception) {
			throw new IllegalStateException("Could not deserialize audit log details.", exception);
		}
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
