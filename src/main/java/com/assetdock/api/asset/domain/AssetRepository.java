package com.assetdock.api.asset.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface AssetRepository {

	boolean existsByOrganizationIdAndAssetTag(UUID organizationId, String normalizedAssetTag);

	Asset save(Asset asset);

	List<Asset> findAllPaginated(UUID organizationId, int limit, int offset, String status, String search, UUID categoryId, UUID locationId);
	long countForOrganization(UUID organizationId, String status, String search, UUID categoryId, UUID locationId);

	List<Asset> findAllPaginatedGlobally(int limit, int offset, String status, String search, UUID categoryId, UUID locationId);
	long countGlobally(String status, String search, UUID categoryId, UUID locationId);

	Optional<Asset> findByIdAndOrganizationId(UUID assetId, UUID organizationId);

	Optional<Asset> findByIdAndOrganizationIdForUpdate(UUID assetId, UUID organizationId);

	Optional<Asset> findById(UUID assetId);

	Asset update(Asset asset);

	/**
	 * Updates only the status and assigned person columns. Used by checkout/checkin
	 * to avoid reconstructing the full Asset record when only these two fields change.
	 */
	void updateStatusAndAssignedPerson(UUID assetId, UUID organizationId, AssetStatus status, UUID assignedPersonId, Instant updatedAt);

	Asset archive(UUID assetId, UUID organizationId, Instant archivedAt, Instant updatedAt);

	Map<AssetStatus, Integer> countByStatusForOrganization(UUID organizationId);
}
