package com.assetdock.api.auth.application;

import com.assetdock.api.auth.infrastructure.JwtProperties;
import com.assetdock.api.auth.infrastructure.JwtTokenService;
import com.assetdock.api.security.auth.AuthenticatedUserPrincipal;
import com.assetdock.api.user.domain.User;
import com.assetdock.api.user.domain.UserRepository;
import com.assetdock.api.user.domain.UserRole;
import com.assetdock.api.user.domain.UserStatus;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

	private static final String RAW_PASSWORD = "S3curePass!";
	private static final Instant NOW = Instant.parse("2026-03-17T12:00:00Z");

	@Mock
	private UserRepository userRepository;

	private AuthenticationService authenticationService;
	private PasswordEncoder passwordEncoder;

	@BeforeEach
	void setUp() {
		passwordEncoder = new BCryptPasswordEncoder();
		Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
		byte[] secret = "test-only-jwt-secret-key-with-32-bytes".getBytes(StandardCharsets.UTF_8);
		SecretKey secretKey = new SecretKeySpec(secret, "HmacSHA256");
		JwtEncoder jwtEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
		JwtTokenService jwtTokenService = new JwtTokenService(
			jwtEncoder,
			new JwtProperties("test-only-jwt-secret-key-with-32-bytes", "assetdock-api", Duration.ofMinutes(15))
		);

		authenticationService = new AuthenticationService(userRepository, passwordEncoder, jwtTokenService, clock);
	}

	@Test
	void shouldLoginSuccessfullyForActiveUser() {
		User activeUser = user(UserStatus.ACTIVE);
		when(userRepository.findByEmail("user@assetdock.dev")).thenReturn(java.util.Optional.of(activeUser));

		LoginResult result = authenticationService.login(new LoginCommand("USER@ASSETDOCK.DEV", RAW_PASSWORD));

		assertThat(result.accessToken()).isNotBlank();
		assertThat(result.expiresInSeconds()).isEqualTo(900);
		assertThat(result.principal()).isEqualTo(AuthenticatedUserPrincipal.from(activeUser));
		verify(userRepository).updateLastLoginAt(eq(activeUser.id()), eq(NOW));
	}

	@Test
	void shouldRejectInvalidCredentials() {
		User activeUser = user(UserStatus.ACTIVE);
		when(userRepository.findByEmail("user@assetdock.dev")).thenReturn(java.util.Optional.of(activeUser));

		assertThatThrownBy(() -> authenticationService.login(new LoginCommand("user@assetdock.dev", "wrong-password")))
			.isInstanceOf(InvalidCredentialsException.class);

		verify(userRepository, never()).updateLastLoginAt(any(), any());
	}

	@Test
	void shouldBlockInactiveUser() {
		User inactiveUser = user(UserStatus.INACTIVE);
		when(userRepository.findByEmail("user@assetdock.dev")).thenReturn(java.util.Optional.of(inactiveUser));

		assertThatThrownBy(() -> authenticationService.login(new LoginCommand("user@assetdock.dev", RAW_PASSWORD)))
			.isInstanceOf(InactiveUserAuthenticationException.class);

		verify(userRepository, never()).updateLastLoginAt(any(), any());
	}

	@Test
	void shouldBlockLockedUser() {
		User lockedUser = user(UserStatus.LOCKED);
		when(userRepository.findByEmail("user@assetdock.dev")).thenReturn(java.util.Optional.of(lockedUser));

		assertThatThrownBy(() -> authenticationService.login(new LoginCommand("user@assetdock.dev", RAW_PASSWORD)))
			.isInstanceOf(LockedUserAuthenticationException.class);

		verify(userRepository, never()).updateLastLoginAt(any(), any());
	}

	private User user(UserStatus status) {
		return new User(
			UUID.fromString("11111111-1111-1111-1111-111111111111"),
			UUID.fromString("22222222-2222-2222-2222-222222222222"),
			"user@assetdock.dev",
			"AssetDock User",
			passwordEncoder.encode(RAW_PASSWORD),
			status,
			Set.of(UserRole.ORG_ADMIN, UserRole.AUDITOR),
			null,
			NOW.minusSeconds(3600),
			NOW.minusSeconds(3600)
		);
	}
}
