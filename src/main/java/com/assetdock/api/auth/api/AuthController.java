package com.assetdock.api.auth.api;

import com.assetdock.api.auth.application.AuthenticationService;
import com.assetdock.api.auth.application.LoginCommand;
import com.assetdock.api.auth.application.LoginResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AuthenticationService authenticationService;

	public AuthController(AuthenticationService authenticationService) {
		this.authenticationService = authenticationService;
	}

	@PostMapping("/login")
	ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
		LoginResult result = authenticationService.login(new LoginCommand(request.email(), request.password()));

		LoginResponse response = new LoginResponse(
			result.accessToken(),
			"Bearer",
			result.expiresInSeconds(),
			new LoginResponse.AuthenticatedUserResponse(
				result.principal().userId(),
				result.principal().organizationId(),
				result.principal().email(),
				result.principal().roles().stream().map(Enum::name).collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new))
			)
		);

		return ResponseEntity.ok(response);
	}
}
