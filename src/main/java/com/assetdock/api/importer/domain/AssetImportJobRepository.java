package com.assetdock.api.importer.domain;

import java.util.Optional;
import java.util.UUID;

public interface AssetImportJobRepository {

	AssetImportJob save(AssetImportJob job);

	AssetImportJob update(AssetImportJob job);

	Optional<AssetImportJob> findById(UUID jobId);

	Optional<AssetImportJob> findByIdAndOrganizationId(UUID jobId, UUID organizationId);
}
