package com.assetdock.api.assignment.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssetAssignmentRepository {

	AssetAssignment save(AssetAssignment assignment);

	Optional<AssetAssignment> findActiveByAssetIdAndOrganizationId(UUID assetId, UUID organizationId);

	List<AssetAssignment> findAllByAssetIdAndOrganizationId(UUID assetId, UUID organizationId);

	AssetAssignment closeActiveAssignment(UUID assignmentId, UUID organizationId, Instant unassignedAt);
}
