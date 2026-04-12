package com.assetdock.api.auth.api;

import com.assetdock.api.auth.application.LoginCommand;
import com.assetdock.api.auth.application.WebAuthenticatedSession;
import com.assetdock.api.auth.application.WebSessionService;
import com.assetdock.api.security.auth.WebSessionAuthenticationToken;
import com.assetdock.api.user.domain.User;
import com.assetdock.api.user.domain.UserRole;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/web/auth")
public class WebAuthController {

	private static final List<UserRole> ROLE_PRECEDENCE = List.of(
		UserRole.SUPER_ADMIN,
		UserRole.ORG_ADMIN,
		UserRole.ASSET_MANAGER,
		UserRole.AUDITOR,
		UserRole.VIEWER
	);

	private final WebSessionService webSessionService;
	private final WebSessionCookieService webSessionCookieService;

	public WebAuthController(
		WebSessionService webSessionService,
		WebSessionCookieService webSessionCookieService
	) {
		this.webSessionService = webSessionService;
		this.webSessionCookieService = webSessionCookieService;
	}

	@PostMapping("/login")
	ResponseEntity<WebSessionResponse> login(
		@Valid @RequestBody LoginRequest request,
		HttpServletResponse response
	) {
		WebAuthenticatedSession authenticatedSession = webSessionService.create(
			new LoginCommand(request.email(), request.password())
		);
		webSessionCookieService.writeLoginCookies(response, authenticatedSession);
		return ResponseEntity.ok(toResponse(authenticatedSession.user()));
	}

	@GetMapping("/me")
	WebSessionResponse me(Authentication authentication) {
		WebSessionAuthenticationToken sessionAuthentication = requireWebSession(authentication);
		return toResponse(sessionAuthentication.user());
	}

	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void logout(Authentication authentication, HttpServletResponse response) {
		WebSessionAuthenticationToken sessionAuthentication = requireWebSession(authentication);
		webSessionService.logout(sessionAuthentication.sessionId());
		webSessionCookieService.clearAuthenticationCookies(response);
	}

	private WebSessionAuthenticationToken requireWebSession(Authentication authentication) {
		if (authentication instanceof WebSessionAuthenticationToken sessionAuthentication) {
			return sessionAuthentication;
		}

		throw new InsufficientAuthenticationException("Web session authentication is required.");
	}

	private WebSessionResponse toResponse(User user) {
		return new WebSessionResponse(new WebSessionResponse.AuthenticatedUserResponse(
			user.id(),
			user.fullName(),
			user.email(),
			resolvePrimaryRole(user),
			user.organizationId()
		));
	}

	private String resolvePrimaryRole(User user) {
		return user.roles().stream()
			.sorted(Comparator.comparingInt(ROLE_PRECEDENCE::indexOf))
			.findFirst()
			.map(Enum::name)
			.orElse("VIEWER");
	}
}
