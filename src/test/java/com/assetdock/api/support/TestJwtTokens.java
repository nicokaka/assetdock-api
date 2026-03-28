package com.assetdock.api.support;

import com.assetdock.api.user.domain.UserRole;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

public final class TestJwtTokens {

	private static final String SECRET = "test-only-jwt-secret-key-with-32-bytes";
	private static final JwtEncoder ENCODER = new NimbusJwtEncoder(
		new ImmutableSecret<>(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
	);

	private TestJwtTokens() {
	}

	public static String issue(
		UUID userId,
		UUID organizationId,
		String email,
		Set<UserRole> roles
	) {
		Instant issuedAt = Instant.now();
		JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
			.issuer("assetdock-api")
			.subject(userId.toString())
			.issuedAt(issuedAt)
			.expiresAt(issuedAt.plus(Duration.ofMinutes(15)))
			.claim("user_id", userId.toString())
			.claim("email", email)
			.claim("roles", roles.stream().map(Enum::name).toList());

		if (organizationId != null) {
			claims.claim("organization_id", organizationId.toString());
		}

		return ENCODER.encode(
			JwtEncoderParameters.from(
				JwsHeader.with(MacAlgorithm.HS256).build(),
				claims.build()
			)
		).getTokenValue();
	}
}
