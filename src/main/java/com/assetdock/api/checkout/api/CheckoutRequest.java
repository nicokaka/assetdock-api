package com.assetdock.api.checkout.api;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record CheckoutRequest(
    @NotNull UUID personId,
    Instant expectedReturnDate,
    String notes
) {
}
