package com.assetdock.api.setup.api;

import com.assetdock.api.setup.application.SetupCommand;
import com.assetdock.api.setup.application.SetupResult;
import com.assetdock.api.setup.application.SetupService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/setup")
public class SetupController {

	private final SetupService setupService;

	public SetupController(SetupService setupService) {
		this.setupService = setupService;
	}

	@GetMapping("/status")
	SetupStatusResponse status() {
		return new SetupStatusResponse(setupService.isConfigured());
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	SetupResponse setup(@Valid @RequestBody SetupRequest request) {
		SetupResult result = setupService.setup(new SetupCommand(
			request.organizationName(),
			request.adminFullName(),
			request.adminEmail(),
			request.adminPassword()
		));

		return new SetupResponse(
			new SetupResponse.OrganizationSummary(
				result.organization().id(),
				result.organization().name()
			),
			new SetupResponse.AdminSummary(
				result.admin().id(),
				result.admin().email(),
				result.admin().fullName()
			)
		);
	}
}
