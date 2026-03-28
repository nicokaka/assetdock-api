package com.assetdock.api.catalog.application;

import com.assetdock.api.audit.application.AuditLogCommand;
import com.assetdock.api.audit.application.AuditLogService;
import com.assetdock.api.audit.domain.AuditEventType;
import com.assetdock.api.catalog.domain.Location;
import com.assetdock.api.catalog.domain.LocationRepository;
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
public class LocationManagementService {

	private final LocationRepository locationRepository;
	private final TenantAccessService tenantAccessService;
	private final AuditLogService auditLogService;
	private final Clock clock;

	public LocationManagementService(
		LocationRepository locationRepository,
		TenantAccessService tenantAccessService,
		AuditLogService auditLogService,
		Clock clock
	) {
		this.locationRepository = locationRepository;
		this.tenantAccessService = tenantAccessService;
		this.auditLogService = auditLogService;
		this.clock = clock;
	}

	@Transactional
	public LocationView create(AuthenticatedUserPrincipal actor, CreateLocationCommand command) {
		UUID organizationId = requireActorOrganizationId(actor);
		tenantAccessService.requireCatalogWriteAccess(actor, organizationId);

		String normalizedName = normalizeName(command.name());
		if (locationRepository.existsByOrganizationIdAndName(organizationId, normalizedName)) {
			throw new CatalogItemAlreadyExistsException("Location", command.name().trim());
		}

		Instant now = Instant.now(clock);
		Location location = new Location(
			UUID.randomUUID(),
			organizationId,
			command.name().trim(),
			normalizeDescription(command.description()),
			command.active(),
			now,
			now
		);

		Location savedLocation = locationRepository.save(location);
		auditLogService.record(new AuditLogCommand(
			organizationId,
			actor.userId(),
			AuditEventType.LOCATION_CREATED,
			"location",
			savedLocation.id(),
			"SUCCESS",
			java.util.Map.of(
				"name", savedLocation.name(),
				"active", savedLocation.active()
			)
		));

		return toView(savedLocation);
	}

	@Transactional(readOnly = true)
	public List<LocationView> list(AuthenticatedUserPrincipal actor) {
		UUID organizationId = requireActorOrganizationId(actor);
		tenantAccessService.requireCatalogReadAccess(actor, organizationId);

		return locationRepository.findAllByOrganizationId(organizationId)
			.stream()
			.map(this::toView)
			.toList();
	}

	@Transactional
	public LocationView update(AuthenticatedUserPrincipal actor, UUID locationId, UpdateLocationCommand command) {
		UUID organizationId = requireActorOrganizationId(actor);
		tenantAccessService.requireCatalogWriteAccess(actor, organizationId);
		requireUpdatePayload(command.name(), command.description(), command.active());

		Location existingLocation = locationRepository.findByIdAndOrganizationId(locationId, organizationId)
			.orElseThrow(() -> new InvalidCatalogRequestException("Location does not exist in the current organization."));

		String name = command.name() == null ? existingLocation.name() : normalizeRequired(command.name(), "name");
		if (!existingLocation.name().equalsIgnoreCase(name)
			&& locationRepository.existsByOrganizationIdAndName(organizationId, normalizeName(name))) {
			throw new CatalogItemAlreadyExistsException("Location", name);
		}

		Instant now = Instant.now(clock);
		Location updatedLocation = new Location(
			existingLocation.id(),
			existingLocation.organizationId(),
			name,
			command.description() == null ? existingLocation.description() : normalizeDescription(command.description()),
			command.active() == null ? existingLocation.active() : command.active(),
			existingLocation.createdAt(),
			now
		);

		Location persistedLocation = locationRepository.update(updatedLocation);
		auditLogService.record(new AuditLogCommand(
			organizationId,
			actor.userId(),
			AuditEventType.LOCATION_UPDATED,
			"location",
			persistedLocation.id(),
			"SUCCESS",
			java.util.Map.of(
				"previousName", existingLocation.name(),
				"newName", persistedLocation.name(),
				"previousActive", existingLocation.active(),
				"newActive", persistedLocation.active()
			)
		));

		return toView(persistedLocation);
	}

	private LocationView toView(Location location) {
		return new LocationView(
			location.id(),
			location.name(),
			location.description(),
			location.active(),
			location.createdAt(),
			location.updatedAt()
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
