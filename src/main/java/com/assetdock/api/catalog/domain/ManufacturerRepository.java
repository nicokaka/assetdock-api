package com.assetdock.api.catalog.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ManufacturerRepository {

	boolean existsByOrganizationIdAndName(UUID organizationId, String normalizedName);

	Manufacturer save(Manufacturer manufacturer);

	Manufacturer update(Manufacturer manufacturer);

	List<Manufacturer> findAllByOrganizationId(UUID organizationId);

	Optional<Manufacturer> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
