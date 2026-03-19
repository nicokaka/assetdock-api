package com.assetdock.api.config;

import com.assetdock.api.organization.domain.Organization;
import com.assetdock.api.organization.domain.OrganizationRepository;
import com.assetdock.api.user.application.InvalidUserRequestException;
import com.assetdock.api.user.domain.User;
import com.assetdock.api.user.domain.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalDevelopmentSeedRunnerTest {

	@Mock
	private OrganizationRepository organizationRepository;

	@Mock
	private UserRepository userRepository;

	private PasswordEncoder passwordEncoder;

	@BeforeEach
	void setUp() {
		passwordEncoder = new BCryptPasswordEncoder();
	}

	@Test
	void shouldUseConfiguredPasswordWhenSeeding() throws Exception {
		LocalDevelopmentSeedRunner runner = new LocalDevelopmentSeedRunner(
			new LocalSeedProperties(
				true,
				"AssetDock Local",
				"assetdock-local",
				"Local Admin",
				"admin@assetdock.dev",
				"ConfiguredSecret123!"
			),
			organizationRepository,
			userRepository,
			passwordEncoder,
			Clock.fixed(Instant.parse("2026-03-18T12:00:00Z"), ZoneOffset.UTC)
		);

		when(organizationRepository.findBySlug("assetdock-local")).thenReturn(Optional.empty());
		when(userRepository.existsByEmail("admin@assetdock.dev")).thenReturn(false);
		when(organizationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		runner.run(new DefaultApplicationArguments(new String[0]));

		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(userCaptor.capture());
		assertThat(passwordEncoder.matches("ConfiguredSecret123!", userCaptor.getValue().passwordHash())).isTrue();
	}

	@Test
	void shouldFailWhenSeedPasswordIsMissing() {
		LocalDevelopmentSeedRunner runner = new LocalDevelopmentSeedRunner(
			new LocalSeedProperties(
				true,
				"AssetDock Local",
				"assetdock-local",
				"Local Admin",
				"admin@assetdock.dev",
				""
			),
			organizationRepository,
			userRepository,
			passwordEncoder,
			Clock.systemUTC()
		);

		assertThatThrownBy(() -> runner.run(new DefaultApplicationArguments(new String[0])))
			.isInstanceOf(InvalidUserRequestException.class)
			.hasMessageContaining("LOCAL_SEED_ADMIN_PASSWORD");

		verify(userRepository, never()).save(any());
	}
}
