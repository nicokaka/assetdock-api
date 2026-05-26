package com.assetdock.api.checkout.application;

import com.assetdock.api.asset.application.AssetNotFoundException;
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
import com.assetdock.api.security.auth.AuthenticatedUserPrincipal;
import com.assetdock.api.security.auth.TenantAccessService;
import com.assetdock.api.user.domain.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class CheckoutService {

    private final AssetCheckoutRepository checkoutRepository;
    private final AssetRepository assetRepository;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;
    private final TenantAccessService tenantAccessService;
    private final Clock clock;

    public CheckoutService(
        AssetCheckoutRepository checkoutRepository,
        AssetRepository assetRepository,
        AuditLogService auditLogService,
        UserRepository userRepository,
        TenantAccessService tenantAccessService,
        Clock clock
    ) {
        this.checkoutRepository = checkoutRepository;
        this.assetRepository = assetRepository;
        this.auditLogService = auditLogService;
        this.userRepository = userRepository;
        this.tenantAccessService = tenantAccessService;
        this.clock = clock;
    }

    public CheckoutView checkout(AuthenticatedUserPrincipal principal, UUID assetId, CheckoutRequest request) {
        UUID organizationId = requireActorOrganizationId(principal);
        // C-1: RBAC — only ORG_ADMIN and ASSET_MANAGER may perform checkouts.
        tenantAccessService.requireAssignmentWriteAccess(principal, organizationId);

        Asset asset = assetRepository.findByIdAndOrganizationIdForUpdate(assetId, organizationId)
            .orElseThrow(AssetNotFoundException::new);

        if (asset.status() != AssetStatus.IN_STOCK) {
            throw new InvalidCheckoutRequestException("Asset must be IN_STOCK to be checked out");
        }

        var assignedUser = userRepository.findById(request.userId())
            .orElseThrow(() -> new InvalidCheckoutRequestException("User not found"));

        if (assignedUser.organizationId() == null || !assignedUser.organizationId().equals(principal.organizationId())) {
            throw new InvalidCheckoutRequestException("User does not belong to your organization");
        }

        Instant now = Instant.now(clock);

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

        // H-1: Dedicated status update — avoids reconstructing the full Asset record.
        assetRepository.updateStatusAndAssignedUser(
            asset.id(), asset.organizationId(), AssetStatus.ASSIGNED, request.userId(), now
        );

        auditLogService.recordInCurrentTransaction(new AuditLogCommand(
            principal.organizationId(),
            principal.userId(),
            AuditEventType.ASSET_CHECKED_OUT,
            "ASSET",
            asset.id(),
            "SUCCESS",
            Map.of("assignedTo", request.userId())
        ));

        return mapToView(checkout);
    }

    public CheckoutView checkin(AuthenticatedUserPrincipal principal, UUID assetId, CheckinRequest request) {
        UUID organizationId = requireActorOrganizationId(principal);
        // C-1: RBAC — only ORG_ADMIN and ASSET_MANAGER may perform checkins.
        tenantAccessService.requireAssignmentWriteAccess(principal, organizationId);

        Asset asset = assetRepository.findByIdAndOrganizationIdForUpdate(assetId, organizationId)
            .orElseThrow(AssetNotFoundException::new);

        if (asset.status() != AssetStatus.ASSIGNED) {
            throw new InvalidCheckoutRequestException("Asset must be ASSIGNED to be checked in");
        }

        // C-2: Acquire a row-level lock on the active checkout record to prevent double-checkin.
        AssetCheckout activeCheckout = checkoutRepository
            .findActiveByAssetIdAndOrganizationIdForUpdate(assetId, organizationId)
            .orElseThrow(() -> new InvalidCheckoutRequestException("No active checkout found for this asset"));

        Instant now = Instant.now(clock);

        String mergedNotes = request.notes() != null
            ? (activeCheckout.notes() != null ? activeCheckout.notes() + "\n" : "") + "Checkin notes: " + request.notes()
            : activeCheckout.notes();

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
            mergedNotes,
            activeCheckout.createdAt()
        );

        checkoutRepository.update(updatedCheckout);

        // H-1: Dedicated status update — avoids reconstructing the full Asset record.
        assetRepository.updateStatusAndAssignedUser(
            asset.id(), asset.organizationId(), AssetStatus.IN_STOCK, null, now
        );

        auditLogService.recordInCurrentTransaction(new AuditLogCommand(
            principal.organizationId(),
            principal.userId(),
            AuditEventType.ASSET_CHECKED_IN,
            "ASSET",
            asset.id(),
            "SUCCESS",
            Map.of("notes", request.notes() != null ? request.notes() : "")
        ));

        return mapToView(updatedCheckout);
    }

	@Transactional(readOnly = true)
	public List<CheckoutView> getHistoryByAssetId(AuthenticatedUserPrincipal principal, UUID assetId) {
        UUID organizationId = requireActorOrganizationId(principal);
        // C-1: RBAC — read access requires at least AUDITOR or ASSET_MANAGER.
        tenantAccessService.requireAssignmentReadAccess(principal, organizationId);

        // Verify the asset belongs to this organization.
        assetRepository.findByIdAndOrganizationId(assetId, organizationId)
            .orElseThrow(AssetNotFoundException::new);

        // L-3: Use Java 21 Stream.toList() instead of Collectors.toList().
        return checkoutRepository.findByAssetIdAndOrganizationIdOrderByCheckedOutAtDesc(assetId, organizationId)
            .stream()
            .map(this::mapToView)
            .toList();
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

    private UUID requireActorOrganizationId(AuthenticatedUserPrincipal actor) {
        if (actor.organizationId() == null) {
            throw new InvalidCheckoutRequestException("Tenant organization context is required for checkout operations.");
        }
        return actor.organizationId();
    }
}
