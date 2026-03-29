package com.assetdock.api.user.application;

import com.assetdock.api.audit.application.AuditContext;
import com.assetdock.api.audit.application.AuditContextProvider;
import com.assetdock.api.audit.application.AuditLogService;
import com.assetdock.api.audit.domain.AuditLogRepository;
import com.assetdock.api.organization.domain.Organization;
import com.assetdock.api.organization.domain.OrganizationRepository;
import com.assetdock.api.security.auth.AuthenticatedUserPrincipal;
import com.assetdock.api.security.auth.TenantAccessService;
import com.assetdock.api.user.domain.User;
import com.assetdock.api.user.domain.UserRepository;
import com.assetdock.api.user.domain.UserRole;
import com.assetdock.api.user.domain.UserStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserManagementServiceTest {

	private static final Instant NOW = Instant.parse("2026-03-18T12:00:00Z");
	private static final UUID ORG_1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID ORG_2 = UUID.fromString("22222222-2222-2222-2222-222222222222");
	private static final UUID USER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

	@Mock
	private UserRepository userRepository;

	@Mock
	private OrganizationRepository organizationRepository;

	private UserManagementService userManagementService;
	private PasswordEncoder passwordEncoder;

	@BeforeEach
	void setUp() {
		passwordEncoder = new BCryptPasswordEncoder();
		AuditLogService auditLogService = new AuditLogService(
			org.mockito.Mockito.mock(AuditLogRepository.class),
			new AuditContextProvider() {
				@Override
				public AuditContext current() {
					return new AuditContext("127.0.0.1", "JUnit", "test-request-id");
				}
			},
			Clock.fixed(NOW, ZoneOffset.UTC)
		);
		userManagementService = new UserManagementService(
			userRepository,
			organizationRepository,
			new TenantAccessService(),
			passwordEncoder,
			auditLogService,
			Clock.fixed(NOW, ZoneOffset.UTC)
		);
	}

	@Test
	void orgAdminShouldCreateUserInOwnOrganization() {
		AuthenticatedUserPrincipal actor = principal(
			UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
			ORG_1,
			Set.of(UserRole.ORG_ADMIN)
		);
		CreateUserCommand command = new CreateUserCommand(
			null,
			"New User",
			"new.user@assetdock.dev",
			"S3curePass!",
			Set.of(UserRole.VIEWER),
			UserStatus.ACTIVE
		);

		when(organizationRepository.findById(ORG_1)).thenReturn(Optional.of(organization(ORG_1)));
		when(userRepository.existsByEmail("new.user@assetdock.dev")).thenReturn(false);
		when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		UserView result = userManagementService.createUser(actor, command);

		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(userCaptor.capture());
		User savedUser = userCaptor.getValue();

		assertThat(savedUser.organizationId()).isEqualTo(ORG_1);
		assertThat(savedUser.email()).isEqualTo("new.user@assetdock.dev");
		assertThat(passwordEncoder.matches("S3curePass!", savedUser.passwordHash())).isTrue();
		assertThat(result.organizationId()).isEqualTo(ORG_1);
	}

	@Test
	void shouldDenyCrossTenantRead() {
		AuthenticatedUserPrincipal actor = principal(
			UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
			ORG_1,
			Set.of(UserRole.ORG_ADMIN)
		);
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(USER_ID, ORG_2, UserStatus.ACTIVE)));

		assertThatThrownBy(() -> userManagementService.getUser(actor, USER_ID))
			.isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void shouldDenyCrossTenantStatusUpdate() {
		AuthenticatedUserPrincipal actor = principal(
			UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
			ORG_1,
			Set.of(UserRole.ORG_ADMIN)
		);
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(USER_ID, ORG_2, UserStatus.ACTIVE)));

		assertThatThrownBy(() -> userManagementService.updateStatus(actor, USER_ID, UserStatus.LOCKED))
			.isInstanceOf(AccessDeniedException.class);

		verify(userRepository, never()).updateStatus(any(), any(), any());
	}

	@Test
	void shouldUpdateRolesForTenantUser() {
		AuthenticatedUserPrincipal actor = principal(
			UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
			ORG_1,
			Set.of(UserRole.ORG_ADMIN)
		);
		User currentUser = user(USER_ID, ORG_1, UserStatus.ACTIVE, Set.of(UserRole.VIEWER));
		User updatedUser = user(USER_ID, ORG_1, UserStatus.ACTIVE, Set.of(UserRole.ASSET_MANAGER, UserRole.AUDITOR));
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(currentUser));
		when(userRepository.updateRoles(eq(USER_ID), eq(Set.of(UserRole.ASSET_MANAGER, UserRole.AUDITOR)), any()))
			.thenReturn(updatedUser);

		UserView result = userManagementService.updateRoles(actor, USER_ID, Set.of(UserRole.ASSET_MANAGER, UserRole.AUDITOR));

		assertThat(result.roles()).containsExactlyInAnyOrder(UserRole.ASSET_MANAGER, UserRole.AUDITOR);
		verify(userRepository).updateRoles(eq(USER_ID), eq(Set.of(UserRole.ASSET_MANAGER, UserRole.AUDITOR)), any());
	}

	@Test
	void shouldPreventRemovingLastEffectiveOrgAdminByRoleChange() {
		AuthenticatedUserPrincipal actor = principal(
			UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
			ORG_1,
			Set.of(UserRole.ORG_ADMIN)
		);
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(USER_ID, ORG_1, UserStatus.ACTIVE)));
		when(userRepository.countActiveUsersByOrganizationIdAndRole(ORG_1, UserRole.ORG_ADMIN)).thenReturn(1L);

		assertThatThrownBy(() -> userManagementService.updateRoles(actor, USER_ID, Set.of(UserRole.VIEWER)))
			.isInstanceOf(InvalidUserRequestException.class)
			.hasMessage("Cannot remove the last effective ORG_ADMIN from the organization.");

		verify(userRepository, never()).updateRoles(any(), any(), any());
	}

	@Test
	void shouldPreventLockingLastEffectiveOrgAdmin() {
		AuthenticatedUserPrincipal actor = principal(
			UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
			ORG_1,
			Set.of(UserRole.ORG_ADMIN)
		);
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(USER_ID, ORG_1, UserStatus.ACTIVE)));
		when(userRepository.countActiveUsersByOrganizationIdAndRole(ORG_1, UserRole.ORG_ADMIN)).thenReturn(1L);

		assertThatThrownBy(() -> userManagementService.updateStatus(actor, USER_ID, UserStatus.LOCKED))
			.isInstanceOf(InvalidUserRequestException.class)
			.hasMessage("Cannot remove the last effective ORG_ADMIN from the organization.");

		verify(userRepository, never()).updateStatus(any(), any(), any());
	}

	@Test
	void shouldPreventAssigningSuperAdminToTenantUser() {
		AuthenticatedUserPrincipal actor = principal(
			UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
			ORG_1,
			Set.of(UserRole.ORG_ADMIN)
		);
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(USER_ID, ORG_1, UserStatus.ACTIVE, Set.of(UserRole.VIEWER))));

		assertThatThrownBy(() -> userManagementService.updateRoles(actor, USER_ID, Set.of(UserRole.SUPER_ADMIN)))
			.isInstanceOf(AccessDeniedException.class)
			.hasMessage("Only SUPER_ADMIN can assign SUPER_ADMIN.");
	}

	@Test
	void auditorShouldReceiveLimitedPayload() {
		AuthenticatedUserPrincipal actor = principal(
			UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
			ORG_1,
			Set.of(UserRole.AUDITOR)
		);
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(USER_ID, ORG_1, UserStatus.ACTIVE)));

		UserView result = userManagementService.getUser(actor, USER_ID);

		assertThat(result.id()).isEqualTo(USER_ID);
		assertThat(result.organizationId()).isNull();
		assertThat(result.createdAt()).isNull();
		assertThat(result.updatedAt()).isNull();
		assertThat(result.email()).isEqualTo("user@assetdock.dev");
		assertThat(result.roles()).contains(UserRole.ORG_ADMIN);
	}

	@Test
	void viewerShouldNotMutateUsers() {
		AuthenticatedUserPrincipal actor = principal(
			UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
			ORG_1,
			Set.of(UserRole.VIEWER)
		);
		when(organizationRepository.findById(ORG_1)).thenReturn(Optional.of(organization(ORG_1)));

		assertThatThrownBy(() -> userManagementService.createUser(
			actor,
			new CreateUserCommand(
				null,
				"New User",
				"new.user@assetdock.dev",
				"S3curePass!",
				Set.of(UserRole.VIEWER),
				UserStatus.ACTIVE
			)
		)).isInstanceOf(AccessDeniedException.class);

		verify(userRepository, never()).save(any());
	}

	private AuthenticatedUserPrincipal principal(UUID userId, UUID organizationId, Set<UserRole> roles) {
		return new AuthenticatedUserPrincipal(userId, organizationId, "actor@assetdock.dev", roles);
	}

	private Organization organization(UUID organizationId) {
		return new Organization(organizationId, "AssetDock", "assetdock", NOW, NOW);
	}

	private User user(UUID userId, UUID organizationId, UserStatus status) {
		return user(userId, organizationId, status, Set.of(UserRole.ORG_ADMIN));
	}

	private User user(UUID userId, UUID organizationId, UserStatus status, Set<UserRole> roles) {
		return new User(
			userId,
			organizationId,
			"user@assetdock.dev",
			"AssetDock User",
			"$2a$10$abcdefghijklmnopqrstuvwxyzABCDE1234567890",
			status,
			roles,
			0,
			null,
			NOW.minusSeconds(3600),
			NOW.minusSeconds(1800)
		);
	}
}
