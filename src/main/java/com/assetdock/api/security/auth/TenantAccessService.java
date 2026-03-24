package com.assetdock.api.security.auth;

import com.assetdock.api.user.domain.UserRole;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class TenantAccessService {

	public void requireOrganizationReadAccess(AuthenticatedUserPrincipal actor, UUID organizationId) {
		if (actor.isSuperAdmin()) {
			return;
		}

		if (actor.organizationId() != null && actor.organizationId().equals(organizationId)) {
			return;
		}

		throw new AccessDeniedException("Cross-tenant access is not allowed.");
	}

	public void requireUserReadAccess(AuthenticatedUserPrincipal actor, UUID targetOrganizationId) {
		if (actor.isSuperAdmin()) {
			return;
		}

		if (!actor.hasRole(UserRole.ORG_ADMIN) && !actor.hasRole(UserRole.AUDITOR)) {
			throw new AccessDeniedException("You do not have permission to read users.");
		}

		if (actor.organizationId() == null || !actor.organizationId().equals(targetOrganizationId)) {
			throw new AccessDeniedException("Cross-tenant access is not allowed.");
		}
	}

	public void requireUserWriteAccess(AuthenticatedUserPrincipal actor, UUID targetOrganizationId) {
		if (actor.isSuperAdmin()) {
			return;
		}

		if (!actor.hasRole(UserRole.ORG_ADMIN)) {
			throw new AccessDeniedException("You do not have permission to manage users.");
		}

		if (actor.organizationId() == null || !actor.organizationId().equals(targetOrganizationId)) {
			throw new AccessDeniedException("Cross-tenant access is not allowed.");
		}
	}

	public void requireCatalogReadAccess(AuthenticatedUserPrincipal actor, UUID targetOrganizationId) {
		if (actor.isSuperAdmin()) {
			return;
		}

		if (!actor.hasRole(UserRole.ORG_ADMIN)
			&& !actor.hasRole(UserRole.ASSET_MANAGER)
			&& !actor.hasRole(UserRole.AUDITOR)
			&& !actor.hasRole(UserRole.VIEWER)) {
			throw new AccessDeniedException("You do not have permission to read catalogs.");
		}

		if (actor.organizationId() == null || !actor.organizationId().equals(targetOrganizationId)) {
			throw new AccessDeniedException("Cross-tenant access is not allowed.");
		}
	}

	public void requireCatalogWriteAccess(AuthenticatedUserPrincipal actor, UUID targetOrganizationId) {
		if (actor.isSuperAdmin()) {
			return;
		}

		if (!actor.hasRole(UserRole.ORG_ADMIN) && !actor.hasRole(UserRole.ASSET_MANAGER)) {
			throw new AccessDeniedException("You do not have permission to manage catalogs.");
		}

		if (actor.organizationId() == null || !actor.organizationId().equals(targetOrganizationId)) {
			throw new AccessDeniedException("Cross-tenant access is not allowed.");
		}
	}

	public void requireAssetReadAccess(AuthenticatedUserPrincipal actor, UUID targetOrganizationId) {
		if (actor.isSuperAdmin()) {
			return;
		}

		if (!actor.hasRole(UserRole.ORG_ADMIN)
			&& !actor.hasRole(UserRole.ASSET_MANAGER)
			&& !actor.hasRole(UserRole.AUDITOR)
			&& !actor.hasRole(UserRole.VIEWER)) {
			throw new AccessDeniedException("You do not have permission to read assets.");
		}

		if (actor.organizationId() == null || !actor.organizationId().equals(targetOrganizationId)) {
			throw new AccessDeniedException("Cross-tenant access is not allowed.");
		}
	}

	public void requireAssetWriteAccess(AuthenticatedUserPrincipal actor, UUID targetOrganizationId) {
		if (actor.isSuperAdmin()) {
			return;
		}

		if (!actor.hasRole(UserRole.ORG_ADMIN) && !actor.hasRole(UserRole.ASSET_MANAGER)) {
			throw new AccessDeniedException("You do not have permission to manage assets.");
		}

		if (actor.organizationId() == null || !actor.organizationId().equals(targetOrganizationId)) {
			throw new AccessDeniedException("Cross-tenant access is not allowed.");
		}
	}
}
