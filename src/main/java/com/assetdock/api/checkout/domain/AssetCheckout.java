package com.assetdock.api.checkout.domain;

import java.time.Instant;
import java.util.UUID;

public record AssetCheckout(
    UUID id,
    UUID organizationId,
    UUID assetId,
    UUID personId,
    Instant checkedOutAt,
    Instant expectedReturnDate,
    Instant checkedInAt,
    UUID checkedOutBy,
    UUID checkedInBy,
    String notes,
    Instant createdAt
) {
}
