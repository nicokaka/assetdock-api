package com.assetdock.api.auth.application;

import com.assetdock.api.auth.infrastructure.JwtTokenService;
import com.assetdock.api.security.auth.AuthenticatedUserPrincipal;
import com.assetdock.api.user.domain.User;
import com.assetdock.api.user.domain.UserRepository;
import com.assetdock.api.user.domain.UserStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenService jwtTokenService;
	private final Clock clock;

	public AuthenticationService(
		UserRepository userRepository,
		PasswordEncoder passwordEncoder,
		JwtTokenService jwtTokenService,
		Clock clock
	) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtTokenService = jwtTokenService;
		this.clock = clock;
	}

	@Transactional
	public LoginResult login(LoginCommand command) {
		String normalizedEmail = normalizeEmail(command.email());
		User user = userRepository.findByEmail(normalizedEmail)
			.orElseThrow(InvalidCredentialsException::new);

		if (!passwordEncoder.matches(command.password(), user.passwordHash())) {
			throw new InvalidCredentialsException();
		}

		if (user.status() == UserStatus.INACTIVE) {
			throw new InactiveUserAuthenticationException();
		}

		if (user.status() == UserStatus.LOCKED) {
			throw new LockedUserAuthenticationException();
		}

		Instant loginAt = Instant.now(clock);
		userRepository.updateLastLoginAt(user.id(), loginAt);

		AuthenticatedUserPrincipal principal = AuthenticatedUserPrincipal.from(user);
		JwtTokenService.IssuedToken issuedToken = jwtTokenService.issue(principal, loginAt);

		return new LoginResult(issuedToken.value(), issuedToken.expiresInSeconds(), principal);
	}

	private String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}
}
