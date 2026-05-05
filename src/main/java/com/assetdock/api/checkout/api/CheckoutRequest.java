package com.assetdock.api.checkout.api;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record CheckoutRequest(
    @NotNull UUID userId,
    Instant expectedReturnDate,
    String notes
) {
}
