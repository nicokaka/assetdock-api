package com.assetdock.api.asset.domain;

import java.util.List;
import java.util.Optional;
import java.time.Instant;
import java.util.UUID;

public interface AssetRepository {

	boolean existsByOrganizationIdAndAssetTag(UUID organizationId, String normalizedAssetTag);

	Asset save(Asset asset);

	List<Asset> findAllPaginated(UUID organizationId, int limit, int offset, String status, String search);

	long countForOrganization(UUID organizationId, String status, String search);


	Optional<Asset> findByIdAndOrganizationId(UUID assetId, UUID organizationId);

	Optional<Asset> findById(UUID assetId);

	Asset update(Asset asset);

	Asset archive(UUID assetId, UUID organizationId, Instant archivedAt, Instant updatedAt);

	java.util.Map<AssetStatus, Integer> countByStatusForOrganization(UUID organizationId);
}
