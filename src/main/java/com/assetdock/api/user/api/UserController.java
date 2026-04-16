package com.assetdock.api.user.api;

import com.assetdock.api.security.auth.AuthenticatedUserPrincipal;
import com.assetdock.api.user.application.CreateUserCommand;
import com.assetdock.api.user.application.UpdateUserProfileCommand;
import com.assetdock.api.user.application.UserManagementService;
import com.assetdock.api.user.application.UserView;
import com.assetdock.api.user.application.UserPageView;
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
import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/users")
public class UserController {

	private final UserManagementService userManagementService;

	public UserController(UserManagementService userManagementService) {
		this.userManagementService = userManagementService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	UserView createUser(
		@AuthenticationPrincipal AuthenticatedUserPrincipal principal,
		@Valid @RequestBody CreateUserRequest request
	) {
		return userManagementService.createUser(
			principal,
			new CreateUserCommand(
				request.organizationId(),
				request.fullName(),
				request.email(),
				request.password(),
				request.roles(),
				request.status()
			)
		);
	}

	@GetMapping
	UserPageView listUsers(
		@AuthenticationPrincipal AuthenticatedUserPrincipal principal,
		@org.springframework.web.bind.annotation.RequestParam(required = false) Integer page,
		@org.springframework.web.bind.annotation.RequestParam(required = false) Integer size,
		@org.springframework.web.bind.annotation.RequestParam(required = false) String search
	) {
		return userManagementService.listUsers(principal, page, size, search);
	}

	@GetMapping("/{id}")
	UserView getUser(
		@PathVariable UUID id,
		@AuthenticationPrincipal AuthenticatedUserPrincipal principal
	) {
		return userManagementService.getUser(principal, id);
	}

	@PatchMapping("/{id}")
	UserView updateProfile(
		@PathVariable UUID id,
		@AuthenticationPrincipal AuthenticatedUserPrincipal principal,
		@Valid @RequestBody UpdateUserProfileRequest request
	) {
		return userManagementService.updateProfile(
			principal,
			id,
			new UpdateUserProfileCommand(
				request.fullName(),
				request.email()
			)
		);
	}

	@PatchMapping("/{id}/roles")
	UserView updateRoles(
		@PathVariable UUID id,
		@AuthenticationPrincipal AuthenticatedUserPrincipal principal,
		@Valid @RequestBody UpdateUserRolesRequest request
	) {
		return userManagementService.updateRoles(principal, id, request.roles());
	}

	@PatchMapping("/{id}/status")
	UserView updateStatus(
		@PathVariable UUID id,
		@AuthenticationPrincipal AuthenticatedUserPrincipal principal,
		@Valid @RequestBody UpdateUserStatusRequest request
	) {
		return userManagementService.updateStatus(principal, id, request.status());
	}
}
