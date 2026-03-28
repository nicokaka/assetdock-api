package com.assetdock.api.asset.application;

import com.assetdock.api.asset.domain.Asset;
import com.assetdock.api.asset.domain.AssetRepository;
import com.assetdock.api.asset.domain.AssetStatus;
import com.assetdock.api.audit.application.AuditLogCommand;
import com.assetdock.api.audit.application.AuditLogService;
import com.assetdock.api.audit.domain.AuditEventType;
import com.assetdock.api.catalog.domain.CategoryRepository;
import com.assetdock.api.catalog.domain.LocationRepository;
import com.assetdock.api.catalog.domain.ManufacturerRepository;
import com.assetdock.api.security.auth.AuthenticatedUserPrincipal;
import com.assetdock.api.security.auth.TenantAccessService;
import com.assetdock.api.user.domain.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssetManagementService {

	private final AssetRepository assetRepository;
	private final CategoryRepository categoryRepository;
	private final ManufacturerRepository manufacturerRepository;
	private final LocationRepository locationRepository;
	private final UserRepository userRepository;
	private final TenantAccessService tenantAccessService;
	private final AuditLogService auditLogService;
	private final Clock clock;

	public AssetManagementService(
		AssetRepository assetRepository,
		CategoryRepository categoryRepository,
		ManufacturerRepository manufacturerRepository,
		LocationRepository locationRepository,
		UserRepository userRepository,
		TenantAccessService tenantAccessService,
		AuditLogService auditLogService,
		Clock clock
	) {
		this.assetRepository = assetRepository;
		this.categoryRepository = categoryRepository;
		this.manufacturerRepository = manufacturerRepository;
		this.locationRepository = locationRepository;
		this.userRepository = userRepository;
		this.tenantAccessService = tenantAccessService;
		this.auditLogService = auditLogService;
		this.clock = clock;
	}

	@Transactional
	public AssetView create(AuthenticatedUserPrincipal actor, CreateAssetCommand command) {
		UUID organizationId = requireActorOrganizationId(actor);
		tenantAccessService.requireAssetWriteAccess(actor, organizationId);

		String assetTag = normalizeRequired(command.assetTag(), "assetTag");
		if (assetRepository.existsByOrganizationIdAndAssetTag(organizationId, assetTag.toLowerCase(java.util.Locale.ROOT))) {
			throw new AssetAlreadyExistsException(assetTag);
		}

		AssetStatus status = command.status() == null ? AssetStatus.IN_STOCK : command.status();
		validateReferences(organizationId, command.categoryId(), command.manufacturerId(), command.currentLocationId(), command.currentAssignedUserId());

		Instant now = Instant.now(clock);
		Asset asset = new Asset(
			UUID.randomUUID(),
			organizationId,
			assetTag,
			normalizeOptional(command.serialNumber()),
			normalizeHostname(command.hostname()),
			normalizeRequired(command.displayName(), "displayName"),
			normalizeOptional(command.description()),
			command.categoryId(),
			command.manufacturerId(),
			command.currentLocationId(),
			command.currentAssignedUserId(),
			status,
			command.purchaseDate(),
			command.warrantyExpiryDate(),
			null,
			now,
			now
		);

		Asset savedAsset = assetRepository.save(asset);
		recordAudit(actor, savedAsset, AuditEventType.ASSET_CREATED, Map.of(
			"assetTag", savedAsset.assetTag(),
			"status", savedAsset.status().name()
		));

		return toView(savedAsset);
	}

	@Transactional(readOnly = true)
	public List<AssetView> list(AuthenticatedUserPrincipal actor) {
		UUID organizationId = requireActorOrganizationId(actor);
		tenantAccessService.requireAssetReadAccess(actor, organizationId);

		return assetRepository.findAllByOrganizationId(organizationId)
			.stream()
			.map(this::toView)
			.toList();
	}

	@Transactional(readOnly = true)
	public AssetView get(AuthenticatedUserPrincipal actor, UUID assetId) {
		Asset asset = findAssetForActor(actor, assetId);
		return toView(asset);
	}

	@Transactional
	public AssetView update(AuthenticatedUserPrincipal actor, UUID assetId, UpdateAssetCommand command) {
		Asset existingAsset = findAssetForActor(actor, assetId);
		tenantAccessService.requireAssetWriteAccess(actor, existingAsset.organizationId());
		ensureNotArchived(existingAsset, "Archived assets cannot be updated.");

		String assetTag = command.assetTag() == null
			? existingAsset.assetTag()
			: normalizeRequired(command.assetTag(), "assetTag");
		if (!existingAsset.assetTag().equalsIgnoreCase(assetTag)
			&& assetRepository.existsByOrganizationIdAndAssetTag(existingAsset.organizationId(), assetTag.toLowerCase(java.util.Locale.ROOT))) {
			throw new AssetAlreadyExistsException(assetTag);
		}

		validateReferences(
			existingAsset.organizationId(),
			command.categoryId() != null ? command.categoryId() : existingAsset.categoryId(),
			command.manufacturerId() != null ? command.manufacturerId() : existingAsset.manufacturerId(),
			command.currentLocationId() != null ? command.currentLocationId() : existingAsset.currentLocationId(),
			command.currentAssignedUserId() != null ? command.currentAssignedUserId() : existingAsset.currentAssignedUserId()
		);

		AssetStatus status = command.status() == null ? existingAsset.status() : command.status();
		Instant now = Instant.now(clock);
		Asset updatedAsset = new Asset(
			existingAsset.id(),
			existingAsset.organizationId(),
			assetTag,
			command.serialNumber() != null ? normalizeOptional(command.serialNumber()) : existingAsset.serialNumber(),
			command.hostname() != null ? normalizeHostname(command.hostname()) : existingAsset.hostname(),
			command.displayName() != null ? normalizeRequired(command.displayName(), "displayName") : existingAsset.displayName(),
			command.description() != null ? normalizeOptional(command.description()) : existingAsset.description(),
			command.categoryId() != null ? command.categoryId() : existingAsset.categoryId(),
			command.manufacturerId() != null ? command.manufacturerId() : existingAsset.manufacturerId(),
			command.currentLocationId() != null ? command.currentLocationId() : existingAsset.currentLocationId(),
			command.currentAssignedUserId() != null ? command.currentAssignedUserId() : existingAsset.currentAssignedUserId(),
			status,
			command.purchaseDate() != null ? command.purchaseDate() : existingAsset.purchaseDate(),
			command.warrantyExpiryDate() != null ? command.warrantyExpiryDate() : existingAsset.warrantyExpiryDate(),
			existingAsset.archivedAt(),
			existingAsset.createdAt(),
			now
		);

		Asset persistedAsset = assetRepository.update(updatedAsset);
		recordAudit(actor, persistedAsset, AuditEventType.ASSET_UPDATED, Map.of(
			"assetTag", persistedAsset.assetTag(),
			"previousStatus", existingAsset.status().name(),
			"newStatus", persistedAsset.status().name()
		));

		return toView(persistedAsset);
	}

	@Transactional
	public AssetView updateStatus(AuthenticatedUserPrincipal actor, UUID assetId, UpdateAssetStatusCommand command) {
		Asset existingAsset = findAssetForActor(actor, assetId);
		tenantAccessService.requireAssetWriteAccess(actor, existingAsset.organizationId());
		ensureNotArchived(existingAsset, "Archived assets cannot change status.");

		Instant now = Instant.now(clock);
		Asset updatedAsset = new Asset(
			existingAsset.id(),
			existingAsset.organizationId(),
			existingAsset.assetTag(),
			existingAsset.serialNumber(),
			existingAsset.hostname(),
			existingAsset.displayName(),
			existingAsset.description(),
			existingAsset.categoryId(),
			existingAsset.manufacturerId(),
			existingAsset.currentLocationId(),
			existingAsset.currentAssignedUserId(),
			command.status(),
			existingAsset.purchaseDate(),
			existingAsset.warrantyExpiryDate(),
			existingAsset.archivedAt(),
			existingAsset.createdAt(),
			now
		);

		Asset persistedAsset = assetRepository.update(updatedAsset);
		recordAudit(actor, persistedAsset, AuditEventType.ASSET_UPDATED, Map.of(
			"assetTag", persistedAsset.assetTag(),
			"previousStatus", existingAsset.status().name(),
			"newStatus", persistedAsset.status().name()
		));

		return toView(persistedAsset);
	}

	@Transactional
	public AssetView archive(AuthenticatedUserPrincipal actor, UUID assetId) {
		Asset existingAsset = findAssetForActor(actor, assetId);
		tenantAccessService.requireAssetWriteAccess(actor, existingAsset.organizationId());

		if (existingAsset.archivedAt() != null) {
			return toView(existingAsset);
		}

		if (existingAsset.status() != AssetStatus.RETIRED && existingAsset.status() != AssetStatus.LOST) {
			throw new InvalidAssetRequestException("Only RETIRED or LOST assets can be archived.");
		}

		Instant now = Instant.now(clock);
		Asset archivedAsset = assetRepository.archive(existingAsset.id(), existingAsset.organizationId(), now, now);
		recordAudit(actor, archivedAsset, AuditEventType.ASSET_ARCHIVED, Map.of(
			"assetTag", archivedAsset.assetTag(),
			"status", archivedAsset.status().name(),
			"archivedAt", archivedAsset.archivedAt().toString()
		));

		return toView(archivedAsset);
	}

	private Asset findAssetForActor(AuthenticatedUserPrincipal actor, UUID assetId) {
		if (actor.isSuperAdmin()) {
			return assetRepository.findById(assetId).orElseThrow(AssetNotFoundException::new);
		}

		UUID organizationId = requireActorOrganizationId(actor);
		tenantAccessService.requireAssetReadAccess(actor, organizationId);
		return assetRepository.findByIdAndOrganizationId(assetId, organizationId).orElseThrow(AssetNotFoundException::new);
	}

	private void validateReferences(
		UUID organizationId,
		UUID categoryId,
		UUID manufacturerId,
		UUID currentLocationId,
		UUID currentAssignedUserId
	) {
		if (categoryId != null && categoryRepository.findByIdAndOrganizationId(categoryId, organizationId).isEmpty()) {
			throw new InvalidAssetRequestException("categoryId must belong to the same organization as the asset.");
		}

		if (manufacturerId != null && manufacturerRepository.findByIdAndOrganizationId(manufacturerId, organizationId).isEmpty()) {
			throw new InvalidAssetRequestException("manufacturerId must belong to the same organization as the asset.");
		}

		if (currentLocationId != null && locationRepository.findByIdAndOrganizationId(currentLocationId, organizationId).isEmpty()) {
			throw new InvalidAssetRequestException("currentLocationId must belong to the same organization as the asset.");
		}

		if (currentAssignedUserId != null) {
			var assignedUser = userRepository.findById(currentAssignedUserId)
				.orElseThrow(() -> new InvalidAssetRequestException("currentAssignedUserId must reference an existing user."));
			if (assignedUser.organizationId() == null || !assignedUser.organizationId().equals(organizationId)) {
				throw new InvalidAssetRequestException("currentAssignedUserId must belong to the same organization as the asset.");
			}
		}
	}

	private void recordAudit(
		AuthenticatedUserPrincipal actor,
		Asset asset,
		AuditEventType eventType,
		Map<String, Object> details
	) {
		auditLogService.record(new AuditLogCommand(
			asset.organizationId(),
			actor.userId(),
			eventType,
			"asset",
			asset.id(),
			"SUCCESS",
			details
		));
	}

	private AssetView toView(Asset asset) {
		return new AssetView(
			asset.id(),
			asset.assetTag(),
			asset.serialNumber(),
			asset.hostname(),
			asset.displayName(),
			asset.description(),
			asset.categoryId(),
			asset.manufacturerId(),
			asset.currentLocationId(),
			asset.currentAssignedUserId(),
			asset.status(),
			asset.purchaseDate(),
			asset.warrantyExpiryDate(),
			asset.archivedAt(),
			asset.createdAt(),
			asset.updatedAt()
		);
	}

	private UUID requireActorOrganizationId(AuthenticatedUserPrincipal actor) {
		if (actor.organizationId() == null) {
			throw new InvalidAssetRequestException("Tenant organization context is required for asset operations.");
		}

		return actor.organizationId();
	}

	private String normalizeRequired(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new InvalidAssetRequestException(fieldName + " is required.");
		}

		return value.trim();
	}

	private String normalizeOptional(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	private String normalizeHostname(String value) {
		String normalized = normalizeOptional(value);
		return normalized == null ? null : normalized.toLowerCase(java.util.Locale.ROOT);
	}

	private void ensureNotArchived(Asset asset, String message) {
		if (asset.archivedAt() != null) {
			throw new InvalidAssetRequestException(message);
		}
	}
}
