package com.assetdock.api.organization.api;

import com.assetdock.api.organization.application.OrganizationQueryService;
import com.assetdock.api.organization.application.OrganizationView;
import com.assetdock.api.security.auth.AuthenticatedUserPrincipal;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/organizations")
public class OrganizationController {

	private final OrganizationQueryService organizationQueryService;

	public OrganizationController(OrganizationQueryService organizationQueryService) {
		this.organizationQueryService = organizationQueryService;
	}

	@GetMapping("/{id}")
	OrganizationView getOrganization(
		@PathVariable UUID id,
		@AuthenticationPrincipal AuthenticatedUserPrincipal principal
	) {
		return organizationQueryService.getOrganization(principal, id);
	}
}
