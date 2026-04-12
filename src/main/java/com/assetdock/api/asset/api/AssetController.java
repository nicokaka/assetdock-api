package com.assetdock.api.asset.api;

import com.assetdock.api.asset.application.AssetManagementService;
import com.assetdock.api.asset.application.AssetView;
import com.assetdock.api.asset.application.CreateAssetCommand;
import com.assetdock.api.asset.application.UpdateAssetCommand;
import com.assetdock.api.asset.application.UpdateAssetStatusCommand;
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

	public AssetController(AssetManagementService assetManagementService) {
		this.assetManagementService = assetManagementService;
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
	List<AssetView> list(@AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
		return assetManagementService.list(principal);
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
}
