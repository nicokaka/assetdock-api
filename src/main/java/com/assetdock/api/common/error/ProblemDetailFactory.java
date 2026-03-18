package com.assetdock.api.common.error;

import java.net.URI;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

@Component
public class ProblemDetailFactory {

	private final Clock clock;

	public ProblemDetailFactory(Clock clock) {
		this.clock = clock;
	}

	public ProblemDetail create(
		HttpStatus status,
		String title,
		String detail,
		String type,
		String path
	) {
		return create(status, title, detail, type, path, Map.of());
	}

	public ProblemDetail create(
		HttpStatus status,
		String title,
		String detail,
		String type,
		String path,
		Map<String, Object> properties
	) {
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
		problemDetail.setTitle(title);
		problemDetail.setType(URI.create(type));
		problemDetail.setProperty("timestamp", OffsetDateTime.now(clock));
		problemDetail.setProperty("path", path);
		properties.forEach(problemDetail::setProperty);
		return problemDetail;
	}
}
