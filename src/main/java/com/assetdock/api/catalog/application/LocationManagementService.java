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

	private String normalizeDescription(String description) {
		return description == null || description.isBlank() ? null : description.trim();
	}
}
