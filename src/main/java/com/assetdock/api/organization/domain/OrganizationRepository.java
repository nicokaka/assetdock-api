package com.assetdock.api.organization.domain;

import java.util.Optional;
import java.util.UUID;

public interface OrganizationRepository {

	Optional<Organization> findById(UUID organizationId);

	Optional<Organization> findBySlug(String slug);

	Organization save(Organization organization);
}
