package com.assetdock.api.catalog.application;

import com.assetdock.api.audit.application.AuditLogCommand;
import com.assetdock.api.audit.application.AuditLogService;
import com.assetdock.api.audit.domain.AuditEventType;
import com.assetdock.api.catalog.domain.Manufacturer;
import com.assetdock.api.catalog.domain.ManufacturerRepository;
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

		return manufacturerRepository.findAllByOrganizationId(organizationId)
			.stream()
			.map(this::toView)
			.toList();
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

	private String normalizeOptional(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
