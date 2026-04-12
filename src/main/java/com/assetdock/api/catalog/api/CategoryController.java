package com.assetdock.api.catalog.api;

import com.assetdock.api.catalog.application.CategoryManagementService;
import com.assetdock.api.catalog.application.CategoryView;
import com.assetdock.api.catalog.application.CreateCategoryCommand;
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
@RequestMapping("/categories")
public class CategoryController {

	private final CategoryManagementService categoryManagementService;

	public CategoryController(CategoryManagementService categoryManagementService) {
		this.categoryManagementService = categoryManagementService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	CategoryView create(
		@AuthenticationPrincipal AuthenticatedUserPrincipal principal,
		@Valid @RequestBody CreateCategoryRequest request
	) {
		return categoryManagementService.create(
			principal,
			new CreateCategoryCommand(
				request.name(),
				request.description(),
				request.active() == null || request.active()
			)
		);
	}

	@GetMapping
	List<CategoryView> list(@AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
		return categoryManagementService.list(principal);
	}

	@PatchMapping("/{id}")
	CategoryView update(
		@PathVariable java.util.UUID id,
		@AuthenticationPrincipal AuthenticatedUserPrincipal principal,
		@Valid @RequestBody UpdateCategoryRequest request
	) {
		return categoryManagementService.update(
			principal,
			id,
			new com.assetdock.api.catalog.application.UpdateCategoryCommand(
				request.name(),
				request.description(),
				request.active()
			)
		);
	}
}
