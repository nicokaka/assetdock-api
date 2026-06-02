package com.assetdock.api.checkout.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssetCheckoutRepository {
    
    AssetCheckout save(AssetCheckout checkout);
    
    AssetCheckout update(AssetCheckout checkout);

    List<AssetCheckout> findByAssetIdAndOrganizationIdOrderByCheckedOutAtDesc(UUID assetId, UUID organizationId);

    List<AssetCheckout> findByPersonIdAndOrganizationIdOrderByCheckedOutAtDesc(UUID personId, UUID organizationId);

    /**
     * Returns the single active (not yet checked-in) checkout record for the given asset,
     * acquiring a row-level lock (FOR UPDATE) to prevent concurrent checkin race conditions.
     */
    Optional<AssetCheckout> findActiveByAssetIdAndOrganizationIdForUpdate(UUID assetId, UUID organizationId);

    int countActiveCheckoutsForOrganization(UUID organizationId);
}
