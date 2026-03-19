package com.assetdock.api.catalog.api;

import com.assetdock.api.catalog.application.CreateLocationCommand;
import com.assetdock.api.catalog.application.LocationManagementService;
import com.assetdock.api.catalog.application.LocationView;
import com.assetdock.api.security.auth.AuthenticatedUserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/locations")
public class LocationController {

	private final LocationManagementService locationManagementService;

	public LocationController(LocationManagementService locationManagementService) {
		this.locationManagementService = locationManagementService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	LocationView create(
		@AuthenticationPrincipal AuthenticatedUserPrincipal principal,
		@Valid @RequestBody CreateLocationRequest request
	) {
		return locationManagementService.create(
			principal,
			new CreateLocationCommand(
				request.name(),
				request.description(),
				request.active() == null || request.active()
			)
		);
	}

	@GetMapping
	List<LocationView> list(@AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
		return locationManagementService.list(principal);
	}
}
