package com.assetdock.api.common.error;

import com.assetdock.api.auth.application.InactiveUserAuthenticationException;
import com.assetdock.api.auth.application.InvalidCredentialsException;
import com.assetdock.api.auth.application.LockedUserAuthenticationException;
import com.assetdock.api.asset.application.AssetAlreadyExistsException;
import com.assetdock.api.asset.application.AssetNotFoundException;
import com.assetdock.api.asset.application.InvalidAssetRequestException;
import com.assetdock.api.catalog.application.CatalogItemAlreadyExistsException;
import com.assetdock.api.catalog.application.InvalidCatalogRequestException;
import com.assetdock.api.organization.application.OrganizationNotFoundException;
import com.assetdock.api.user.application.EmailAlreadyInUseException;
import com.assetdock.api.user.application.InvalidUserRequestException;
import com.assetdock.api.user.application.UserNotFoundException;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.springframework.http.converter.HttpMessageNotReadableException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private final ProblemDetailFactory problemDetailFactory;

	public GlobalExceptionHandler(ProblemDetailFactory problemDetailFactory) {
		this.problemDetailFactory = problemDetailFactory;
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ProblemDetail> handleValidationException(
		MethodArgumentNotValidException exception,
		WebRequest request
	) {
		List<ValidationViolation> violations = exception.getBindingResult()
			.getFieldErrors()
			.stream()
			.map(this::toViolation)
			.toList();

		ProblemDetail problemDetail = problemDetailFactory.create(
			HttpStatus.BAD_REQUEST,
			"Validation failed",
			"One or more request fields are invalid.",
			"urn:assetdock:problem:validation-error",
			extractPath(request),
			Map.of("violations", violations)
		);

		return ResponseEntity.badRequest().body(problemDetail);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	ResponseEntity<ProblemDetail> handleConstraintViolation(
		ConstraintViolationException exception,
		WebRequest request
	) {
		List<ValidationViolation> violations = exception.getConstraintViolations()
			.stream()
			.map(violation -> new ValidationViolation(violation.getPropertyPath().toString(), violation.getMessage()))
			.toList();

		ProblemDetail problemDetail = problemDetailFactory.create(
			HttpStatus.BAD_REQUEST,
			"Validation failed",
			"One or more request parameters are invalid.",
			"urn:assetdock:problem:constraint-violation",
			extractPath(request),
			Map.of("violations", violations)
		);

		return ResponseEntity.badRequest().body(problemDetail);
	}

	@ExceptionHandler({
		HttpMessageNotReadableException.class,
		MethodArgumentTypeMismatchException.class,
		InvalidFormatException.class
	})
	ResponseEntity<ProblemDetail> handleBadRequest(Exception exception, WebRequest request) {
		ProblemDetail problemDetail = problemDetailFactory.create(
			HttpStatus.BAD_REQUEST,
			"Malformed request",
			"The request could not be understood. Check the payload and parameter formats.",
			"urn:assetdock:problem:malformed-request",
			extractPath(request)
		);

		return ResponseEntity.badRequest().body(problemDetail);
	}

	@ExceptionHandler(NoResourceFoundException.class)
	ResponseEntity<ProblemDetail> handleNotFound(NoResourceFoundException exception, WebRequest request) {
		ProblemDetail problemDetail = problemDetailFactory.create(
			HttpStatus.NOT_FOUND,
			"Resource not found",
			"The requested resource does not exist.",
			"urn:assetdock:problem:not-found",
			extractPath(request)
		);

		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
	}

	@ExceptionHandler(AccessDeniedException.class)
	ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException exception, WebRequest request) {
		ProblemDetail problemDetail = problemDetailFactory.create(
			HttpStatus.FORBIDDEN,
			"Access denied",
			"You do not have permission to access this resource.",
			"urn:assetdock:problem:access-denied",
			extractPath(request)
		);

		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problemDetail);
	}

	@ExceptionHandler(InvalidCredentialsException.class)
	ResponseEntity<ProblemDetail> handleInvalidCredentials(
		InvalidCredentialsException exception,
		WebRequest request
	) {
		ProblemDetail problemDetail = problemDetailFactory.create(
			HttpStatus.UNAUTHORIZED,
			"Authentication failed",
			"Invalid email or password.",
			"urn:assetdock:problem:invalid-credentials",
			extractPath(request)
		);

		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problemDetail);
	}

	@ExceptionHandler(InactiveUserAuthenticationException.class)
	ResponseEntity<ProblemDetail> handleInactiveUser(
		InactiveUserAuthenticationException exception,
		WebRequest request
	) {
		ProblemDetail problemDetail = problemDetailFactory.create(
			HttpStatus.FORBIDDEN,
			"Authentication blocked",
			"Inactive users cannot authenticate.",
			"urn:assetdock:problem:user-inactive",
			extractPath(request)
		);

		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problemDetail);
	}

	@ExceptionHandler(LockedUserAuthenticationException.class)
	ResponseEntity<ProblemDetail> handleLockedUser(
		LockedUserAuthenticationException exception,
		WebRequest request
	) {
		ProblemDetail problemDetail = problemDetailFactory.create(
			HttpStatus.FORBIDDEN,
			"Authentication blocked",
			"Locked users cannot authenticate.",
			"urn:assetdock:problem:user-locked",
			extractPath(request)
		);

		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problemDetail);
	}

	@ExceptionHandler(OrganizationNotFoundException.class)
	ResponseEntity<ProblemDetail> handleOrganizationNotFound(
		OrganizationNotFoundException exception,
		WebRequest request
	) {
		ProblemDetail problemDetail = problemDetailFactory.create(
			HttpStatus.NOT_FOUND,
			"Organization not found",
			"The requested organization does not exist.",
			"urn:assetdock:problem:organization-not-found",
			extractPath(request)
		);

		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
	}

	@ExceptionHandler(UserNotFoundException.class)
	ResponseEntity<ProblemDetail> handleUserNotFound(UserNotFoundException exception, WebRequest request) {
		ProblemDetail problemDetail = problemDetailFactory.create(
			HttpStatus.NOT_FOUND,
			"User not found",
			"The requested user does not exist.",
			"urn:assetdock:problem:user-not-found",
			extractPath(request)
		);

		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
	}

	@ExceptionHandler(AssetNotFoundException.class)
	ResponseEntity<ProblemDetail> handleAssetNotFound(AssetNotFoundException exception, WebRequest request) {
		ProblemDetail problemDetail = problemDetailFactory.create(
			HttpStatus.NOT_FOUND,
			"Asset not found",
			"The requested asset does not exist.",
			"urn:assetdock:problem:asset-not-found",
			extractPath(request)
		);

		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
	}

	@ExceptionHandler(EmailAlreadyInUseException.class)
	ResponseEntity<ProblemDetail> handleEmailAlreadyInUse(
		EmailAlreadyInUseException exception,
		WebRequest request
	) {
		ProblemDetail problemDetail = problemDetailFactory.create(
			HttpStatus.CONFLICT,
			"Email already in use",
			"The provided email is already associated with another user.",
			"urn:assetdock:problem:email-already-in-use",
			extractPath(request)
		);

		return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
	}

	@ExceptionHandler(InvalidUserRequestException.class)
	ResponseEntity<ProblemDetail> handleInvalidUserRequest(
		InvalidUserRequestException exception,
		WebRequest request
	) {
		ProblemDetail problemDetail = problemDetailFactory.create(
			HttpStatus.BAD_REQUEST,
			"Invalid user request",
			exception.getMessage(),
			"urn:assetdock:problem:invalid-user-request",
			extractPath(request)
		);

		return ResponseEntity.badRequest().body(problemDetail);
	}

	@ExceptionHandler(CatalogItemAlreadyExistsException.class)
	ResponseEntity<ProblemDetail> handleCatalogItemAlreadyExists(
		CatalogItemAlreadyExistsException exception,
		WebRequest request
	) {
		ProblemDetail problemDetail = problemDetailFactory.create(
			HttpStatus.CONFLICT,
			"Catalog item already exists",
			exception.getMessage(),
			"urn:assetdock:problem:catalog-item-already-exists",
			extractPath(request)
		);

		return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
	}

	@ExceptionHandler(InvalidCatalogRequestException.class)
	ResponseEntity<ProblemDetail> handleInvalidCatalogRequest(
		InvalidCatalogRequestException exception,
		WebRequest request
	) {
		ProblemDetail problemDetail = problemDetailFactory.create(
			HttpStatus.BAD_REQUEST,
			"Invalid catalog request",
			exception.getMessage(),
			"urn:assetdock:problem:invalid-catalog-request",
			extractPath(request)
		);

		return ResponseEntity.badRequest().body(problemDetail);
	}

	@ExceptionHandler(AssetAlreadyExistsException.class)
	ResponseEntity<ProblemDetail> handleAssetAlreadyExists(
		AssetAlreadyExistsException exception,
		WebRequest request
	) {
		ProblemDetail problemDetail = problemDetailFactory.create(
			HttpStatus.CONFLICT,
			"Asset already exists",
			exception.getMessage(),
			"urn:assetdock:problem:asset-already-exists",
			extractPath(request)
		);

		return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
	}

	@ExceptionHandler(InvalidAssetRequestException.class)
	ResponseEntity<ProblemDetail> handleInvalidAssetRequest(
		InvalidAssetRequestException exception,
		WebRequest request
	) {
		ProblemDetail problemDetail = problemDetailFactory.create(
			HttpStatus.BAD_REQUEST,
			"Invalid asset request",
			exception.getMessage(),
			"urn:assetdock:problem:invalid-asset-request",
			extractPath(request)
		);

		return ResponseEntity.badRequest().body(problemDetail);
	}

	@ExceptionHandler(ErrorResponseException.class)
	ResponseEntity<ProblemDetail> handleFrameworkError(ErrorResponseException exception, WebRequest request) {
		HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
		String detail = exception.getBody().getDetail() != null
			? exception.getBody().getDetail()
			: "The request could not be processed.";

		ProblemDetail problemDetail = problemDetailFactory.create(
			status,
			"Request failed",
			detail,
			"urn:assetdock:problem:request-failed",
			extractPath(request)
		);

		return ResponseEntity.status(status).body(problemDetail);
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ProblemDetail> handleUnexpectedException(Exception exception, WebRequest request) {
		ProblemDetail problemDetail = problemDetailFactory.create(
			HttpStatus.INTERNAL_SERVER_ERROR,
			"Unexpected error",
			"An unexpected internal error occurred.",
			"urn:assetdock:problem:internal-error",
			extractPath(request)
		);

		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail);
	}

	private ValidationViolation toViolation(FieldError fieldError) {
		return new ValidationViolation(fieldError.getField(), fieldError.getDefaultMessage());
	}

	private String extractPath(WebRequest request) {
		return ((ServletWebRequest) request).getRequest().getRequestURI();
	}
}
