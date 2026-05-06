package com.assetdock.api.asset.api;

import com.assetdock.api.asset.application.AssetManagementService;
import com.assetdock.api.asset.application.AssetView;
import com.assetdock.api.asset.application.AssetPageView;
import com.assetdock.api.asset.application.CreateAssetCommand;
import com.assetdock.api.asset.application.UpdateAssetCommand;
import com.assetdock.api.asset.application.UpdateAssetStatusCommand;
import com.assetdock.api.asset.application.TimelineEventView;
import com.assetdock.api.asset.application.AssetLabelService;
import com.assetdock.api.asset.application.AssetTimelineService;
import com.assetdock.api.security.auth.AuthenticatedUserPrincipal;
import jakarta.validation.Valid;
import java.util.List;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/assets")
public class AssetController {

	private final AssetManagementService assetManagementService;
	private final AssetLabelService assetLabelService;
	private final AssetTimelineService assetTimelineService;

	public AssetController(
		AssetManagementService assetManagementService,
		AssetLabelService assetLabelService,
		AssetTimelineService assetTimelineService
	) {
		this.assetManagementService = assetManagementService;
		this.assetLabelService = assetLabelService;
		this.assetTimelineService = assetTimelineService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	AssetView create(
		@AuthenticationPrincipal AuthenticatedUserPrincipal principal,
		@Valid @RequestBody CreateAssetRequest request
	) {
		return assetManagementService.create(
			principal,
			new CreateAssetCommand(
				request.assetTag(),
				request.serialNumber(),
				request.hostname(),
				request.displayName(),
				request.description(),
				request.categoryId(),
				request.manufacturerId(),
				request.currentLocationId(),
				request.currentAssignedUserId(),
				request.status(),
				request.purchaseDate(),
				request.warrantyExpiryDate()
			)
		);
	}

	@GetMapping
	AssetPageView list(
		@AuthenticationPrincipal AuthenticatedUserPrincipal principal,
		@org.springframework.web.bind.annotation.RequestParam(required = false) Integer page,
		@org.springframework.web.bind.annotation.RequestParam(required = false) Integer size,
		@org.springframework.web.bind.annotation.RequestParam(required = false) String status,
		@org.springframework.web.bind.annotation.RequestParam(required = false) String search
	) {
		return assetManagementService.list(principal, page, size, status, search);
	}

	@GetMapping("/{id}")
	AssetView get(
		@PathVariable UUID id,
		@AuthenticationPrincipal AuthenticatedUserPrincipal principal
	) {
		return assetManagementService.get(principal, id);
	}

	@PatchMapping("/{id}")
	AssetView update(
		@PathVariable UUID id,
		@AuthenticationPrincipal AuthenticatedUserPrincipal principal,
		@RequestBody UpdateAssetRequest request
	) {
		return assetManagementService.update(
			principal,
			id,
			new UpdateAssetCommand(
				request.assetTag(),
				request.serialNumber(),
				request.hostname(),
				request.displayName(),
				request.description(),
				request.categoryId(),
				request.manufacturerId(),
				request.currentLocationId(),
				request.currentAssignedUserId(),
				request.status(),
				request.purchaseDate(),
				request.warrantyExpiryDate()
			)
		);
	}

	@PatchMapping("/{id}/status")
	AssetView updateStatus(
		@PathVariable UUID id,
		@AuthenticationPrincipal AuthenticatedUserPrincipal principal,
		@Valid @RequestBody UpdateAssetStatusRequest request
	) {
		return assetManagementService.updateStatus(principal, id, new UpdateAssetStatusCommand(request.status()));
	}

	@PatchMapping("/{id}/archive")
	AssetView archive(
		@PathVariable UUID id,
		@AuthenticationPrincipal AuthenticatedUserPrincipal principal
	) {
		return assetManagementService.archive(principal, id);
	}

	@GetMapping("/{id}/qr-code")
	org.springframework.http.ResponseEntity<byte[]> getQrCode(
		@PathVariable UUID id,
		@AuthenticationPrincipal AuthenticatedUserPrincipal principal
	) {
		// Verify access by getting the asset
		AssetView asset = assetManagementService.get(principal, id);
		byte[] qrCode = assetLabelService.generateQrCodePng(asset.assetTag());
		
		return org.springframework.http.ResponseEntity.ok()
			.contentType(org.springframework.http.MediaType.IMAGE_PNG)
			.body(qrCode);
	}

	@GetMapping("/{id}/label")
	org.springframework.http.ResponseEntity<byte[]> getLabel(
		@PathVariable UUID id,
		@AuthenticationPrincipal AuthenticatedUserPrincipal principal
	) {
		// Verify access
		AssetView asset = assetManagementService.get(principal, id);
		byte[] pdf = assetLabelService.generateLabelPdf(asset.assetTag(), asset.displayName(), asset.serialNumber());
		
		return org.springframework.http.ResponseEntity.ok()
			.contentType(org.springframework.http.MediaType.APPLICATION_PDF)
			.header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"label-" + asset.assetTag().replaceAll("[\"\\r\\n]", "_") + ".pdf\"")
			.body(pdf);
	}

	@GetMapping("/{id}/timeline")
	public List<TimelineEventView> getAssetTimeline(
		@PathVariable UUID id,
		@AuthenticationPrincipal AuthenticatedUserPrincipal principal
	) {
		return assetTimelineService.getAssetTimeline(principal, id);
	}
}
