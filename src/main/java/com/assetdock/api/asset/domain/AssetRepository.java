package com.assetdock.api.asset.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssetRepository {

	boolean existsByOrganizationIdAndAssetTag(UUID organizationId, String normalizedAssetTag);

	Asset save(Asset asset);

	List<Asset> findAllByOrganizationId(UUID organizationId);

	Optional<Asset> findByIdAndOrganizationId(UUID assetId, UUID organizationId);

	Optional<Asset> findById(UUID assetId);

	Asset update(Asset asset);
}
