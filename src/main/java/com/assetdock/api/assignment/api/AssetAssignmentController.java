package com.assetdock.api.assignment.api;

import com.assetdock.api.assignment.application.AssignAssetCommand;
import com.assetdock.api.assignment.application.AssetAssignmentManagementService;
import com.assetdock.api.assignment.application.AssetAssignmentView;
import com.assetdock.api.security.auth.AuthenticatedUserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/assets/{assetId}")
public class AssetAssignmentController {

	private final AssetAssignmentManagementService assetAssignmentManagementService;

	public AssetAssignmentController(AssetAssignmentManagementService assetAssignmentManagementService) {
		this.assetAssignmentManagementService = assetAssignmentManagementService;
	}

	@PostMapping("/assignments")
	@ResponseStatus(HttpStatus.CREATED)
	AssetAssignmentView assign(
		@PathVariable UUID assetId,
		@AuthenticationPrincipal AuthenticatedUserPrincipal principal,
		@Valid @RequestBody AssignAssetRequest request
	) {
		return assetAssignmentManagementService.assign(
			principal,
			assetId,
			new AssignAssetCommand(request.userId(), request.locationId(), request.notes())
		);
	}

	@PostMapping("/unassign")
	AssetAssignmentView unassign(
		@PathVariable UUID assetId,
		@AuthenticationPrincipal AuthenticatedUserPrincipal principal
	) {
		return assetAssignmentManagementService.unassign(principal, assetId);
	}

	@GetMapping("/assignments")
	List<AssetAssignmentView> list(
		@PathVariable UUID assetId,
		@AuthenticationPrincipal AuthenticatedUserPrincipal principal
	) {
		return assetAssignmentManagementService.list(principal, assetId);
	}
}
