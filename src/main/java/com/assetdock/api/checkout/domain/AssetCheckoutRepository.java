package com.assetdock.api.checkout.domain;

import java.util.List;
import java.util.UUID;

public interface AssetCheckoutRepository {
    
    AssetCheckout save(AssetCheckout checkout);
    
    AssetCheckout update(AssetCheckout checkout);

    List<AssetCheckout> findByAssetIdOrderByCheckedOutAtDesc(UUID assetId);

    List<AssetCheckout> findByUserIdOrderByCheckedOutAtDesc(UUID userId);
}
