package com.assetdock.api.common.error;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import static org.assertj.core.api.Assertions.assertThat;

class ProblemDetailFactoryTest {

	@Test
	void shouldCreateProblemDetailWithDefaultMetadata() {
		Clock fixedClock = Clock.fixed(Instant.parse("2026-03-17T00:00:00Z"), ZoneOffset.UTC);
		ProblemDetailFactory factory = new ProblemDetailFactory(fixedClock);

		ProblemDetail problemDetail = factory.create(
			HttpStatus.BAD_REQUEST,
			"Validation failed",
			"Payload is invalid.",
			"urn:assetdock:problem:validation-error",
			"/api/assets"
		);

		assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
		assertThat(problemDetail.getTitle()).isEqualTo("Validation failed");
		assertThat(problemDetail.getDetail()).isEqualTo("Payload is invalid.");
		assertThat(problemDetail.getType()).hasToString("urn:assetdock:problem:validation-error");
		assertThat(problemDetail.getProperties())
			.containsEntry("path", "/api/assets")
			.containsKey("timestamp");
	}
}
