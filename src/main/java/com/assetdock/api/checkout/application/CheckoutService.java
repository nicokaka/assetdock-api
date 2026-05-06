package com.assetdock.api.checkout.application;

import com.assetdock.api.asset.domain.Asset;
import com.assetdock.api.asset.domain.AssetRepository;
import com.assetdock.api.asset.domain.AssetStatus;
import com.assetdock.api.audit.application.AuditLogCommand;
import com.assetdock.api.audit.application.AuditLogService;
import com.assetdock.api.audit.domain.AuditEventType;
import com.assetdock.api.checkout.api.CheckinRequest;
import com.assetdock.api.checkout.api.CheckoutRequest;
import com.assetdock.api.checkout.domain.AssetCheckout;
import com.assetdock.api.checkout.domain.AssetCheckoutRepository;
import com.assetdock.api.asset.application.AssetNotFoundException;
import com.assetdock.api.security.auth.AuthenticatedUserPrincipal;
import com.assetdock.api.user.domain.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class CheckoutService {

    private final AssetCheckoutRepository checkoutRepository;
    private final AssetRepository assetRepository;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;

    public CheckoutService(
        AssetCheckoutRepository checkoutRepository,
        AssetRepository assetRepository,
        AuditLogService auditLogService,
        UserRepository userRepository
    ) {
        this.checkoutRepository = checkoutRepository;
        this.assetRepository = assetRepository;
        this.auditLogService = auditLogService;
        this.userRepository = userRepository;
    }

    public CheckoutView checkout(AuthenticatedUserPrincipal principal, UUID assetId, CheckoutRequest request) {
        Asset asset = assetRepository.findByIdAndOrganizationId(assetId, principal.organizationId())
            .orElseThrow(AssetNotFoundException::new);

        if (asset.status() != AssetStatus.IN_STOCK) {
            throw new InvalidCheckoutRequestException("Asset must be IN_STOCK to be checked out");
        }

        var assignedUser = userRepository.findById(request.userId())
            .orElseThrow(() -> new InvalidCheckoutRequestException("User not found"));
            
        if (assignedUser.organizationId() == null || !assignedUser.organizationId().equals(principal.organizationId())) {
            throw new InvalidCheckoutRequestException("User does not belong to your organization");
        }

        Instant now = Instant.now();

        AssetCheckout checkout = new AssetCheckout(
            UUID.randomUUID(),
            principal.organizationId(),
            assetId,
            request.userId(),
            now,
            request.expectedReturnDate(),
            null,
            principal.userId(),
            null,
            request.notes(),
            now
        );

        checkout = checkoutRepository.save(checkout);

        // Update asset status
        Asset updatedAsset = new Asset(
            asset.id(),
            asset.organizationId(),
            asset.assetTag(),
            asset.serialNumber(),
            asset.hostname(),
            asset.displayName(),
            asset.description(),
            asset.categoryId(),
            asset.manufacturerId(),
            asset.currentLocationId(),
            request.userId(),
            AssetStatus.ASSIGNED,
            asset.purchaseDate(),
            asset.warrantyExpiryDate(),
            asset.archivedAt(),
            asset.createdAt(),
            now
        );
        assetRepository.update(updatedAsset);

        auditLogService.record(new AuditLogCommand(
            principal.organizationId(),
            principal.userId(),
            AuditEventType.ASSET_CHECKED_OUT,
            "ASSET",
            asset.id(),
            "SUCCESS",
            java.util.Map.of("assignedTo", request.userId())
        ));

        return mapToView(checkout);
    }

    public CheckoutView checkin(AuthenticatedUserPrincipal principal, UUID assetId, CheckinRequest request) {
        Asset asset = assetRepository.findByIdAndOrganizationId(assetId, principal.organizationId())
            .orElseThrow(AssetNotFoundException::new);

        if (asset.status() != AssetStatus.ASSIGNED) {
            throw new InvalidCheckoutRequestException("Asset must be ASSIGNED to be checked in");
        }

        List<AssetCheckout> checkouts = checkoutRepository.findByAssetIdAndOrganizationIdOrderByCheckedOutAtDesc(assetId, principal.organizationId());
        
        AssetCheckout activeCheckout = checkouts.stream()
            .filter(c -> c.checkedInAt() == null)
            .findFirst()
            .orElseThrow(() -> new InvalidCheckoutRequestException("No active checkout found for this asset"));

        Instant now = Instant.now();

        AssetCheckout updatedCheckout = new AssetCheckout(
            activeCheckout.id(),
            activeCheckout.organizationId(),
            activeCheckout.assetId(),
            activeCheckout.userId(),
            activeCheckout.checkedOutAt(),
            activeCheckout.expectedReturnDate(),
            now,
            activeCheckout.checkedOutBy(),
            principal.userId(),
            request.notes() != null
                ? (activeCheckout.notes() != null ? activeCheckout.notes() + "\n" : "") + "Checkin notes: " + request.notes()
                : activeCheckout.notes(),
            activeCheckout.createdAt()
        );

        checkoutRepository.update(updatedCheckout);

        // Update asset status
        Asset updatedAsset = new Asset(
            asset.id(),
            asset.organizationId(),
            asset.assetTag(),
            asset.serialNumber(),
            asset.hostname(),
            asset.displayName(),
            asset.description(),
            asset.categoryId(),
            asset.manufacturerId(),
            asset.currentLocationId(),
            null,
            AssetStatus.IN_STOCK,
            asset.purchaseDate(),
            asset.warrantyExpiryDate(),
            asset.archivedAt(),
            asset.createdAt(),
            now
        );
        assetRepository.update(updatedAsset);

        auditLogService.record(new AuditLogCommand(
            principal.organizationId(),
            principal.userId(),
            AuditEventType.ASSET_CHECKED_IN,
            "ASSET",
            asset.id(),
            "SUCCESS",
            java.util.Map.of("notes", request.notes() != null ? request.notes() : "")
        ));

        return mapToView(updatedCheckout);
    }

    public List<CheckoutView> getHistoryByAssetId(AuthenticatedUserPrincipal principal, UUID assetId) {
        // Verify access to asset
        Asset asset = assetRepository.findByIdAndOrganizationId(assetId, principal.organizationId())
            .orElseThrow(AssetNotFoundException::new);

        return checkoutRepository.findByAssetIdAndOrganizationIdOrderByCheckedOutAtDesc(assetId, principal.organizationId())
            .stream()
            .map(this::mapToView)
            .collect(Collectors.toList());
    }

    private CheckoutView mapToView(AssetCheckout checkout) {
        return new CheckoutView(
            checkout.id(),
            checkout.assetId(),
            checkout.userId(),
            checkout.checkedOutAt(),
            checkout.expectedReturnDate(),
            checkout.checkedInAt(),
            checkout.checkedOutBy(),
            checkout.checkedInBy(),
            checkout.notes()
        );
    }
}
