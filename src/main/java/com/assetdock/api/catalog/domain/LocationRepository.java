package com.assetdock.api.catalog.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LocationRepository {

	boolean existsByOrganizationIdAndName(UUID organizationId, String normalizedName);

	Location save(Location location);

	Location update(Location location);

	List<Location> findAllByOrganizationId(UUID organizationId);

	Optional<Location> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
