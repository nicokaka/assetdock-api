package com.assetdock.api.catalog.application;

import com.assetdock.api.audit.application.AuditLogCommand;
import com.assetdock.api.audit.application.AuditLogService;
import com.assetdock.api.audit.domain.AuditEventType;
import com.assetdock.api.catalog.domain.Category;
import com.assetdock.api.catalog.domain.CategoryRepository;
import com.assetdock.api.common.query.QueryLimits;
import com.assetdock.api.security.auth.AuthenticatedUserPrincipal;
import com.assetdock.api.security.auth.TenantAccessService;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryManagementService {

	private final CategoryRepository categoryRepository;
	private final TenantAccessService tenantAccessService;
	private final AuditLogService auditLogService;
	private final Clock clock;

	public CategoryManagementService(
		CategoryRepository categoryRepository,
		TenantAccessService tenantAccessService,
		AuditLogService auditLogService,
		Clock clock
	) {
		this.categoryRepository = categoryRepository;
		this.tenantAccessService = tenantAccessService;
		this.auditLogService = auditLogService;
		this.clock = clock;
	}

	@Transactional
	public CategoryView create(AuthenticatedUserPrincipal actor, CreateCategoryCommand command) {
		UUID organizationId = requireActorOrganizationId(actor);
		tenantAccessService.requireCatalogWriteAccess(actor, organizationId);

		String normalizedName = normalizeName(command.name());
		if (categoryRepository.existsByOrganizationIdAndName(organizationId, normalizedName)) {
			throw new CatalogItemAlreadyExistsException("Category", command.name().trim());
		}

		Instant now = Instant.now(clock);
		Category category = new Category(
			UUID.randomUUID(),
			organizationId,
			command.name().trim(),
			normalizeDescription(command.description()),
			command.active(),
			now,
			now
		);

		Category savedCategory = categoryRepository.save(category);
		auditLogService.record(new AuditLogCommand(
			organizationId,
			actor.userId(),
			AuditEventType.CATEGORY_CREATED,
			"category",
			savedCategory.id(),
			"SUCCESS",
			java.util.Map.of(
				"name", savedCategory.name(),
				"active", savedCategory.active()
			)
		));

		return toView(savedCategory);
	}

	@Transactional(readOnly = true)
	public List<CategoryView> list(AuthenticatedUserPrincipal actor) {
		UUID organizationId = requireActorOrganizationId(actor);
		tenantAccessService.requireCatalogReadAccess(actor, organizationId);

		return categoryRepository.findAllByOrganizationId(organizationId, QueryLimits.DEFAULT_LIST_LIMIT)
			.stream()
			.map(this::toView)
			.toList();
	}

	@Transactional
	public CategoryView update(AuthenticatedUserPrincipal actor, UUID categoryId, UpdateCategoryCommand command) {
		UUID organizationId = requireActorOrganizationId(actor);
		tenantAccessService.requireCatalogWriteAccess(actor, organizationId);
		requireUpdatePayload(command.name(), command.description(), command.active());

		Category existingCategory = categoryRepository.findByIdAndOrganizationId(categoryId, organizationId)
			.orElseThrow(() -> new InvalidCatalogRequestException("Category does not exist in the current organization."));

		String name = command.name() == null ? existingCategory.name() : normalizeRequired(command.name(), "name");
		if (!existingCategory.name().equalsIgnoreCase(name)
			&& categoryRepository.existsByOrganizationIdAndName(organizationId, normalizeName(name))) {
			throw new CatalogItemAlreadyExistsException("Category", name);
		}

		Instant now = Instant.now(clock);
		Category updatedCategory = new Category(
			existingCategory.id(),
			existingCategory.organizationId(),
			name,
			command.description() == null ? existingCategory.description() : normalizeDescription(command.description()),
			command.active() == null ? existingCategory.active() : command.active(),
			existingCategory.createdAt(),
			now
		);

		Category persistedCategory = categoryRepository.update(updatedCategory);
		auditLogService.record(new AuditLogCommand(
			organizationId,
			actor.userId(),
			AuditEventType.CATEGORY_UPDATED,
			"category",
			persistedCategory.id(),
			"SUCCESS",
			java.util.Map.of(
				"previousName", existingCategory.name(),
				"newName", persistedCategory.name(),
				"previousActive", existingCategory.active(),
				"newActive", persistedCategory.active()
			)
		));

		return toView(persistedCategory);
	}

	private CategoryView toView(Category category) {
		return new CategoryView(
			category.id(),
			category.name(),
			category.description(),
			category.active(),
			category.createdAt(),
			category.updatedAt()
		);
	}

	private UUID requireActorOrganizationId(AuthenticatedUserPrincipal actor) {
		if (actor.organizationId() == null) {
			throw new InvalidCatalogRequestException("Tenant organization context is required for catalog operations.");
		}

		return actor.organizationId();
	}

	private String normalizeName(String name) {
		return name.trim().toLowerCase(Locale.ROOT);
	}

	private String normalizeRequired(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new InvalidCatalogRequestException(fieldName + " is required.");
		}

		return value.trim();
	}

	private String normalizeDescription(String description) {
		return description == null || description.isBlank() ? null : description.trim();
	}

	private void requireUpdatePayload(String name, String description, Boolean active) {
		if (name == null && description == null && active == null) {
			throw new InvalidCatalogRequestException("At least one field must be provided.");
		}
	}
}
