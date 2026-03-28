package com.assetdock.api.common.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public final class JdbcColumnReaders {

	private JdbcColumnReaders() {
	}

	public static Instant getInstant(ResultSet resultSet, String columnLabel) throws SQLException {
		OffsetDateTime value = resultSet.getObject(columnLabel, OffsetDateTime.class);
		return value == null ? null : value.toInstant();
	}

	public static OffsetDateTime toOffsetDateTime(Instant value) {
		return value == null ? null : value.atOffset(ZoneOffset.UTC);
	}
}
