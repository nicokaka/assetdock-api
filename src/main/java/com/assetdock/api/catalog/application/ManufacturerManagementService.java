package com.assetdock.api.catalog.application;

import com.assetdock.api.audit.application.AuditLogCommand;
import com.assetdock.api.audit.application.AuditLogService;
import com.assetdock.api.audit.domain.AuditEventType;
import com.assetdock.api.catalog.domain.Manufacturer;
import com.assetdock.api.catalog.domain.ManufacturerRepository;
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
public class ManufacturerManagementService {

	private final ManufacturerRepository manufacturerRepository;
	private final TenantAccessService tenantAccessService;
	private final AuditLogService auditLogService;
	private final Clock clock;

	public ManufacturerManagementService(
		ManufacturerRepository manufacturerRepository,
		TenantAccessService tenantAccessService,
		AuditLogService auditLogService,
		Clock clock
	) {
		this.manufacturerRepository = manufacturerRepository;
		this.tenantAccessService = tenantAccessService;
		this.auditLogService = auditLogService;
		this.clock = clock;
	}

	@Transactional
	public ManufacturerView create(AuthenticatedUserPrincipal actor, CreateManufacturerCommand command) {
		UUID organizationId = requireActorOrganizationId(actor);
		tenantAccessService.requireCatalogWriteAccess(actor, organizationId);

		String normalizedName = normalizeName(command.name());
		if (manufacturerRepository.existsByOrganizationIdAndName(organizationId, normalizedName)) {
			throw new CatalogItemAlreadyExistsException("Manufacturer", command.name().trim());
		}

		Instant now = Instant.now(clock);
		Manufacturer manufacturer = new Manufacturer(
			UUID.randomUUID(),
			organizationId,
			command.name().trim(),
			normalizeOptional(command.description()),
			normalizeOptional(command.website()),
			command.active(),
			now,
			now
		);

		Manufacturer savedManufacturer = manufacturerRepository.save(manufacturer);
		auditLogService.record(new AuditLogCommand(
			organizationId,
			actor.userId(),
			AuditEventType.MANUFACTURER_CREATED,
			"manufacturer",
			savedManufacturer.id(),
			"SUCCESS",
			java.util.Map.of(
				"name", savedManufacturer.name(),
				"active", savedManufacturer.active()
			)
		));

		return toView(savedManufacturer);
	}

	@Transactional(readOnly = true)
	public List<ManufacturerView> list(AuthenticatedUserPrincipal actor) {
		UUID organizationId = requireActorOrganizationId(actor);
		tenantAccessService.requireCatalogReadAccess(actor, organizationId);

		return manufacturerRepository.findAllByOrganizationId(organizationId, QueryLimits.DEFAULT_LIST_LIMIT)
			.stream()
			.map(this::toView)
			.toList();
	}

	@Transactional
	public ManufacturerView update(AuthenticatedUserPrincipal actor, UUID manufacturerId, UpdateManufacturerCommand command) {
		UUID organizationId = requireActorOrganizationId(actor);
		tenantAccessService.requireCatalogWriteAccess(actor, organizationId);
		requireUpdatePayload(command.name(), command.description(), command.website(), command.active());

		Manufacturer existingManufacturer = manufacturerRepository.findByIdAndOrganizationId(manufacturerId, organizationId)
			.orElseThrow(() -> new InvalidCatalogRequestException("Manufacturer does not exist in the current organization."));

		String name = command.name() == null ? existingManufacturer.name() : normalizeRequired(command.name(), "name");
		if (!existingManufacturer.name().equalsIgnoreCase(name)
			&& manufacturerRepository.existsByOrganizationIdAndName(organizationId, normalizeName(name))) {
			throw new CatalogItemAlreadyExistsException("Manufacturer", name);
		}

		Instant now = Instant.now(clock);
		Manufacturer updatedManufacturer = new Manufacturer(
			existingManufacturer.id(),
			existingManufacturer.organizationId(),
			name,
			command.description() == null ? existingManufacturer.description() : normalizeOptional(command.description()),
			command.website() == null ? existingManufacturer.website() : normalizeOptional(command.website()),
			command.active() == null ? existingManufacturer.active() : command.active(),
			existingManufacturer.createdAt(),
			now
		);

		Manufacturer persistedManufacturer = manufacturerRepository.update(updatedManufacturer);
		auditLogService.record(new AuditLogCommand(
			organizationId,
			actor.userId(),
			AuditEventType.MANUFACTURER_UPDATED,
			"manufacturer",
			persistedManufacturer.id(),
			"SUCCESS",
			java.util.Map.of(
				"previousName", existingManufacturer.name(),
				"newName", persistedManufacturer.name(),
				"previousActive", existingManufacturer.active(),
				"newActive", persistedManufacturer.active()
			)
		));

		return toView(persistedManufacturer);
	}

	private ManufacturerView toView(Manufacturer manufacturer) {
		return new ManufacturerView(
			manufacturer.id(),
			manufacturer.name(),
			manufacturer.description(),
			manufacturer.website(),
			manufacturer.active(),
			manufacturer.createdAt(),
			manufacturer.updatedAt()
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

	private String normalizeOptional(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	private void requireUpdatePayload(String name, String description, String website, Boolean active) {
		if (name == null && description == null && website == null && active == null) {
			throw new InvalidCatalogRequestException("At least one field must be provided.");
		}
	}
}
