package com.assetdock.api.checkout.application;

import java.time.Instant;
import java.util.UUID;

public record CheckoutView(
    UUID id,
    UUID assetId,
    UUID userId,
    Instant checkedOutAt,
    Instant expectedReturnDate,
    Instant checkedInAt,
    UUID checkedOutBy,
    UUID checkedInBy,
    String notes
) {
}
