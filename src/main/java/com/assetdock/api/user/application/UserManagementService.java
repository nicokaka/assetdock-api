package com.assetdock.api.user.application;

import com.assetdock.api.audit.application.AuditLogCommand;
import com.assetdock.api.audit.application.AuditLogService;
import com.assetdock.api.audit.domain.AuditEventType;
import com.assetdock.api.organization.application.OrganizationNotFoundException;
import com.assetdock.api.organization.domain.OrganizationRepository;
import com.assetdock.api.security.auth.AuthenticatedUserPrincipal;
import com.assetdock.api.security.auth.TenantAccessService;
import com.assetdock.api.user.domain.User;
import com.assetdock.api.user.domain.UserRepository;
import com.assetdock.api.user.domain.UserRole;
import com.assetdock.api.user.domain.UserStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserManagementService {

	private static final Logger LOGGER = LoggerFactory.getLogger(UserManagementService.class);

	private final UserRepository userRepository;
	private final OrganizationRepository organizationRepository;
	private final TenantAccessService tenantAccessService;
	private final PasswordEncoder passwordEncoder;
	private final AuditLogService auditLogService;
	private final Clock clock;

	public UserManagementService(
		UserRepository userRepository,
		OrganizationRepository organizationRepository,
		TenantAccessService tenantAccessService,
		PasswordEncoder passwordEncoder,
		AuditLogService auditLogService,
		Clock clock
	) {
		this.userRepository = userRepository;
		this.organizationRepository = organizationRepository;
		this.tenantAccessService = tenantAccessService;
		this.passwordEncoder = passwordEncoder;
		this.auditLogService = auditLogService;
		this.clock = clock;
	}

	@Transactional
	public UserView createUser(AuthenticatedUserPrincipal actor, CreateUserCommand command) {
		validateRoles(command.roles(), actor);

		UUID targetOrganizationId = resolveTargetOrganizationId(actor, command.organizationId(), command.roles());
		if (targetOrganizationId != null) {
			organizationRepository.findById(targetOrganizationId).orElseThrow(OrganizationNotFoundException::new);
			tenantAccessService.requireUserWriteAccess(actor, targetOrganizationId);
		}

		String normalizedEmail = normalizeEmail(command.email());
		if (userRepository.existsByEmail(normalizedEmail)) {
			throw new EmailAlreadyInUseException();
		}

		Instant now = Instant.now(clock);
		User user = new User(
			UUID.randomUUID(),
			targetOrganizationId,
			normalizedEmail,
			command.fullName().trim(),
			passwordEncoder.encode(command.password()),
			command.status(),
			Set.copyOf(command.roles()),
			null,
			now,
			now
		);

		User savedUser = userRepository.save(user);
		LOGGER.info(
			"user_management action=create_user actor_id={} target_user_id={} target_organization_id={}",
			actor.userId(),
			savedUser.id(),
			savedUser.organizationId()
		);
		auditLogService.record(new AuditLogCommand(
			savedUser.organizationId(),
			actor.userId(),
			AuditEventType.USER_CREATED,
			"user",
			savedUser.id(),
			"SUCCESS",
			java.util.Map.of(
				"email", savedUser.email(),
				"status", savedUser.status().name(),
				"roles", savedUser.roles().stream().map(Enum::name).toList()
			)
		));

		return toView(savedUser, actor);
	}

	@Transactional(readOnly = true)
	public List<UserView> listUsers(AuthenticatedUserPrincipal actor) {
		List<User> users = actor.isSuperAdmin()
			? userRepository.findAll()
			: userRepository.findAllByOrganizationId(requireActorOrganizationId(actor));

		if (!actor.isSuperAdmin() && !actor.hasRole(UserRole.ORG_ADMIN) && !actor.hasRole(UserRole.AUDITOR)) {
			throw new org.springframework.security.access.AccessDeniedException("You do not have permission to read users.");
		}

		return users.stream()
			.map(user -> {
				tenantAccessService.requireUserReadAccess(actor, user.organizationId());
				return toView(user, actor);
			})
			.toList();
	}

	@Transactional(readOnly = true)
	public UserView getUser(AuthenticatedUserPrincipal actor, UUID userId) {
		User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
		tenantAccessService.requireUserReadAccess(actor, user.organizationId());
		return toView(user, actor);
	}

	@Transactional
	public UserView updateRoles(AuthenticatedUserPrincipal actor, UUID userId, Set<UserRole> roles) {
		User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
		tenantAccessService.requireUserWriteAccess(actor, user.organizationId());
		validateRoleUpdate(actor, user, roles);

		Set<UserRole> normalizedRoles = Set.copyOf(roles);
		if (user.roles().equals(normalizedRoles)) {
			return toView(user, actor);
		}

		guardAdministrativeContinuity(user, user.status(), normalizedRoles);

		Instant updatedAt = Instant.now(clock);
		User updatedUser = userRepository.updateRoles(userId, normalizedRoles, updatedAt);
		LOGGER.info(
			"user_management action=update_roles actor_id={} target_user_id={} previous_roles={} new_roles={}",
			actor.userId(),
			userId,
			user.roles(),
			updatedUser.roles()
		);
		auditLogService.record(new AuditLogCommand(
			updatedUser.organizationId(),
			actor.userId(),
			AuditEventType.USER_ROLES_UPDATED,
			"user",
			updatedUser.id(),
			"SUCCESS",
			java.util.Map.of(
				"previousRoles", user.roles().stream().map(Enum::name).sorted().toList(),
				"newRoles", updatedUser.roles().stream().map(Enum::name).sorted().toList()
			)
		));

		return toView(updatedUser, actor);
	}

	@Transactional
	public UserView updateStatus(AuthenticatedUserPrincipal actor, UUID userId, UserStatus status) {
		User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
		tenantAccessService.requireUserWriteAccess(actor, user.organizationId());

		if (user.status() == status) {
			return toView(user, actor);
		}

		guardAdministrativeContinuity(user, status, user.roles());

		Instant updatedAt = Instant.now(clock);
		User updatedUser = userRepository.updateStatus(userId, status, updatedAt);
		LOGGER.info(
			"user_management action=update_status actor_id={} target_user_id={} new_status={}",
			actor.userId(),
			userId,
			status
		);
		auditLogService.record(new AuditLogCommand(
			updatedUser.organizationId(),
			actor.userId(),
			resolveStatusAuditEvent(user.status(), updatedUser.status()),
			"user",
			updatedUser.id(),
			"SUCCESS",
			java.util.Map.of(
				"previousStatus", user.status().name(),
				"newStatus", updatedUser.status().name()
			)
		));

		return toView(updatedUser, actor);
	}

	private UUID resolveTargetOrganizationId(
		AuthenticatedUserPrincipal actor,
		UUID requestedOrganizationId,
		Set<UserRole> requestedRoles
	) {
		if (requestedRoles.contains(UserRole.SUPER_ADMIN)) {
			if (!actor.isSuperAdmin()) {
				throw new org.springframework.security.access.AccessDeniedException("Only SUPER_ADMIN can assign SUPER_ADMIN.");
			}

			if (requestedRoles.size() > 1) {
				throw new InvalidUserRequestException("SUPER_ADMIN users cannot be combined with tenant roles.");
			}

			if (requestedOrganizationId != null) {
				throw new InvalidUserRequestException("SUPER_ADMIN users must not belong to an organization.");
			}

			return null;
		}

		if (actor.isSuperAdmin()) {
			if (requestedOrganizationId == null) {
				throw new InvalidUserRequestException("organizationId is required for tenant-scoped users.");
			}

			return requestedOrganizationId;
		}

		UUID actorOrganizationId = requireActorOrganizationId(actor);
		if (requestedOrganizationId != null && !requestedOrganizationId.equals(actorOrganizationId)) {
			throw new org.springframework.security.access.AccessDeniedException("Cross-tenant access is not allowed.");
		}

		return actorOrganizationId;
	}

	private void validateRoles(Set<UserRole> roles, AuthenticatedUserPrincipal actor) {
		if (roles == null || roles.isEmpty()) {
			throw new InvalidUserRequestException("At least one role must be provided.");
		}

		if (!actor.isSuperAdmin() && roles.contains(UserRole.SUPER_ADMIN)) {
			throw new org.springframework.security.access.AccessDeniedException("Only SUPER_ADMIN can assign SUPER_ADMIN.");
		}
	}

	private void validateRoleUpdate(AuthenticatedUserPrincipal actor, User user, Set<UserRole> roles) {
		validateRoles(roles, actor);

		if (user.organizationId() == null) {
			if (!roles.contains(UserRole.SUPER_ADMIN) || roles.size() > 1) {
				throw new InvalidUserRequestException("Global users must keep the SUPER_ADMIN role only.");
			}

			return;
		}

		if (roles.contains(UserRole.SUPER_ADMIN)) {
			throw new InvalidUserRequestException("Tenant-scoped users cannot be assigned the SUPER_ADMIN role.");
		}
	}

	private void guardAdministrativeContinuity(User user, UserStatus requestedStatus, Set<UserRole> requestedRoles) {
		if (isLastActiveOrgAdminBeingRemoved(user, requestedStatus, requestedRoles)) {
			throw new InvalidUserRequestException("Cannot remove the last effective ORG_ADMIN from the organization.");
		}

		if (isLastActiveSuperAdminBeingRemoved(user, requestedStatus, requestedRoles)) {
			throw new InvalidUserRequestException("Cannot remove the last effective SUPER_ADMIN.");
		}
	}

	private boolean isLastActiveOrgAdminBeingRemoved(User user, UserStatus requestedStatus, Set<UserRole> requestedRoles) {
		if (user.organizationId() == null) {
			return false;
		}

		boolean currentlyEffective = user.status() == UserStatus.ACTIVE && user.roles().contains(UserRole.ORG_ADMIN);
		boolean remainsEffective = requestedStatus == UserStatus.ACTIVE && requestedRoles.contains(UserRole.ORG_ADMIN);
		if (!currentlyEffective || remainsEffective) {
			return false;
		}

		return userRepository.countActiveUsersByOrganizationIdAndRole(user.organizationId(), UserRole.ORG_ADMIN) <= 1;
	}

	private boolean isLastActiveSuperAdminBeingRemoved(User user, UserStatus requestedStatus, Set<UserRole> requestedRoles) {
		boolean currentlyEffective = user.status() == UserStatus.ACTIVE && user.roles().contains(UserRole.SUPER_ADMIN);
		boolean remainsEffective = requestedStatus == UserStatus.ACTIVE && requestedRoles.contains(UserRole.SUPER_ADMIN);
		if (!currentlyEffective || remainsEffective) {
			return false;
		}

		return userRepository.countActiveUsersByRole(UserRole.SUPER_ADMIN) <= 1;
	}

	private AuditEventType resolveStatusAuditEvent(UserStatus previousStatus, UserStatus newStatus) {
		if (newStatus == UserStatus.INACTIVE) {
			return AuditEventType.USER_DISABLED;
		}

		if (newStatus == UserStatus.LOCKED) {
			return AuditEventType.USER_LOCKED;
		}

		if (previousStatus == UserStatus.INACTIVE && newStatus == UserStatus.ACTIVE) {
			return AuditEventType.USER_REACTIVATED;
		}

		if (previousStatus == UserStatus.LOCKED && newStatus == UserStatus.ACTIVE) {
			return AuditEventType.USER_UNLOCKED;
		}

		return AuditEventType.USER_UPDATED;
	}

	private UserView toView(User user, AuthenticatedUserPrincipal actor) {
		boolean limited = actor.hasRole(UserRole.AUDITOR) && !actor.hasRole(UserRole.ORG_ADMIN) && !actor.isSuperAdmin();
		return new UserView(
			user.id(),
			limited ? null : user.organizationId(),
			user.fullName(),
			user.email(),
			user.status(),
			Set.copyOf(user.roles()),
			limited ? null : user.createdAt(),
			limited ? null : user.updatedAt()
		);
	}

	private UUID requireActorOrganizationId(AuthenticatedUserPrincipal actor) {
		if (actor.organizationId() == null) {
			throw new InvalidUserRequestException("Tenant organization context is required.");
		}

		return actor.organizationId();
	}

	private String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}
}
