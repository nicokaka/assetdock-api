package com.assetdock.api.catalog.api;

import com.assetdock.api.catalog.application.CreateManufacturerCommand;
import com.assetdock.api.catalog.application.ManufacturerManagementService;
import com.assetdock.api.catalog.application.UpdateManufacturerCommand;
import com.assetdock.api.catalog.application.ManufacturerView;
import com.assetdock.api.security.auth.AuthenticatedUserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/manufacturers")
public class ManufacturerController {

	private final ManufacturerManagementService manufacturerManagementService;

	public ManufacturerController(ManufacturerManagementService manufacturerManagementService) {
		this.manufacturerManagementService = manufacturerManagementService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	ManufacturerView create(
		@AuthenticationPrincipal AuthenticatedUserPrincipal principal,
		@Valid @RequestBody CreateManufacturerRequest request
	) {
		return manufacturerManagementService.create(
			principal,
			new CreateManufacturerCommand(
				request.name(),
				request.description(),
				request.website(),
				request.active() == null || request.active()
			)
		);
	}

	@GetMapping
	List<ManufacturerView> list(@AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
		return manufacturerManagementService.list(principal);
	}

	@PatchMapping("/{id}")
	ManufacturerView update(
		@PathVariable java.util.UUID id,
		@AuthenticationPrincipal AuthenticatedUserPrincipal principal,
		@Valid @RequestBody UpdateManufacturerRequest request
	) {
		return manufacturerManagementService.update(
			principal,
			id,
			new UpdateManufacturerCommand(
				request.name(),
				request.description(),
				request.website(),
				request.active()
			)
		);
	}
}
