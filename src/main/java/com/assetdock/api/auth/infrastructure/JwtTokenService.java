package com.assetdock.api.auth.infrastructure;

import com.assetdock.api.security.auth.AuthenticatedUserPrincipal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

	private final JwtEncoder jwtEncoder;
	private final JwtProperties jwtProperties;

	public JwtTokenService(JwtEncoder jwtEncoder, JwtProperties jwtProperties) {
		this.jwtEncoder = jwtEncoder;
		this.jwtProperties = jwtProperties;
	}

	public IssuedToken issue(AuthenticatedUserPrincipal principal, Instant issuedAt) {
		Instant expiresAt = issuedAt.plus(jwtProperties.accessTokenTtl());
		JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder()
			.issuer(jwtProperties.issuer())
			.subject(principal.userId().toString())
			.issuedAt(issuedAt)
			.expiresAt(expiresAt)
			.claim("user_id", principal.userId().toString())
			.claim("email", principal.email())
			.claim("roles", principal.roles().stream().map(Enum::name).toList());

		if (principal.organizationId() != null) {
			claimsBuilder.claim("organization_id", principal.organizationId().toString());
		}

		String tokenValue = jwtEncoder.encode(
			JwtEncoderParameters.from(
				JwsHeader.with(MacAlgorithm.HS256).build(),
				claimsBuilder.build()
			)
		).getTokenValue();

		return new IssuedToken(tokenValue, jwtProperties.accessTokenTtl().toSeconds());
	}

	public record IssuedToken(String value, long expiresInSeconds) {
	}
}
