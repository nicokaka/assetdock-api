package com.assetdock.api.catalog.domain;

import java.util.List;
import java.util.UUID;

public interface CategoryRepository {

	boolean existsByOrganizationIdAndName(UUID organizationId, String normalizedName);

	Category save(Category category);

	List<Category> findAllByOrganizationId(UUID organizationId);
}
