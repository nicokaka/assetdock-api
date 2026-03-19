package com.assetdock.api.organization.application;

import com.assetdock.api.organization.domain.Organization;
import com.assetdock.api.organization.domain.OrganizationRepository;
import com.assetdock.api.security.auth.AuthenticatedUserPrincipal;
import com.assetdock.api.security.auth.TenantAccessService;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationQueryService {

	private final OrganizationRepository organizationRepository;
	private final TenantAccessService tenantAccessService;

	public OrganizationQueryService(
		OrganizationRepository organizationRepository,
		TenantAccessService tenantAccessService
	) {
		this.organizationRepository = organizationRepository;
		this.tenantAccessService = tenantAccessService;
	}

	@Transactional(readOnly = true)
	public OrganizationView getOrganization(AuthenticatedUserPrincipal actor, UUID organizationId) {
		Organization organization = organizationRepository.findById(organizationId)
			.orElseThrow(OrganizationNotFoundException::new);

		tenantAccessService.requireOrganizationReadAccess(actor, organization.id());

		return new OrganizationView(
			organization.id(),
			organization.name(),
			organization.slug(),
			organization.createdAt(),
			organization.updatedAt()
		);
	}
}
