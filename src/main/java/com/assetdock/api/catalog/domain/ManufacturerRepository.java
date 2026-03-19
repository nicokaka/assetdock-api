package com.assetdock.api.catalog.domain;

import java.util.List;
import java.util.UUID;

public interface ManufacturerRepository {

	boolean existsByOrganizationIdAndName(UUID organizationId, String normalizedName);

	Manufacturer save(Manufacturer manufacturer);

	List<Manufacturer> findAllByOrganizationId(UUID organizationId);
}
