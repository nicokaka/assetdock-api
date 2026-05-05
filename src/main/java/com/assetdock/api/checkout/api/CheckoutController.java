package com.assetdock.api.checkout.api;

import com.assetdock.api.checkout.application.CheckoutService;
import com.assetdock.api.checkout.application.CheckoutView;
import com.assetdock.api.security.auth.AuthenticatedUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/assets/{assetId}")
public class CheckoutController {

    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public CheckoutView checkout(
        @PathVariable UUID assetId,
        @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
        @Valid @RequestBody CheckoutRequest request
    ) {
        return checkoutService.checkout(principal, assetId, request);
    }

    @PostMapping("/checkin")
    public CheckoutView checkin(
        @PathVariable UUID assetId,
        @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
        @Valid @RequestBody CheckinRequest request
    ) {
        return checkoutService.checkin(principal, assetId, request);
    }

    @GetMapping("/checkouts")
    public List<CheckoutView> getHistory(
        @PathVariable UUID assetId,
        @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        return checkoutService.getHistoryByAssetId(principal, assetId);
    }
}
