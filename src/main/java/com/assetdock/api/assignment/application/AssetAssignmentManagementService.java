package com.assetdock.api.assignment.application;

import com.assetdock.api.asset.application.AssetNotFoundException;
import com.assetdock.api.asset.domain.Asset;
import com.assetdock.api.asset.domain.AssetRepository;
import com.assetdock.api.asset.domain.AssetStatus;
import com.assetdock.api.assignment.domain.AssetAssignment;
import com.assetdock.api.assignment.domain.AssetAssignmentRepository;
import com.assetdock.api.audit.application.AuditLogCommand;
import com.assetdock.api.audit.application.AuditLogService;
import com.assetdock.api.audit.domain.AuditEventType;
import com.assetdock.api.catalog.domain.LocationRepository;
import com.assetdock.api.common.query.QueryLimits;
import com.assetdock.api.security.auth.AuthenticatedUserPrincipal;
import com.assetdock.api.security.auth.TenantAccessService;
import com.assetdock.api.person.domain.PersonRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssetAssignmentManagementService {

	private final AssetAssignmentRepository assetAssignmentRepository;
	private final AssetRepository assetRepository;
	private final PersonRepository personRepository;
	private final LocationRepository locationRepository;
	private final TenantAccessService tenantAccessService;
	private final AuditLogService auditLogService;
	private final Clock clock;

	public AssetAssignmentManagementService(
		AssetAssignmentRepository assetAssignmentRepository,
		AssetRepository assetRepository,
		PersonRepository personRepository,
		LocationRepository locationRepository,
		TenantAccessService tenantAccessService,
		AuditLogService auditLogService,
		Clock clock
	) {
		this.assetAssignmentRepository = assetAssignmentRepository;
		this.assetRepository = assetRepository;
		this.personRepository = personRepository;
		this.locationRepository = locationRepository;
		this.tenantAccessService = tenantAccessService;
		this.auditLogService = auditLogService;
		this.clock = clock;
	}

	@Transactional
	public AssetAssignmentView assign(
		AuthenticatedUserPrincipal actor,
		UUID assetId,
		AssignAssetCommand command
	) {
		Asset asset = findAssetForActor(actor, assetId, true);
		validateAssignableAsset(asset);
		validateAssignee(asset.organizationId(), command.personId());
		validateLocation(asset.organizationId(), command.locationId());

		if (assetAssignmentRepository.findActiveByAssetIdAndOrganizationId(asset.id(), asset.organizationId()).isPresent()) {
			throw new AssignmentAlreadyActiveException();
		}

		Instant now = Instant.now(clock);
		AssetAssignment assignment = new AssetAssignment(
			UUID.randomUUID(),
			asset.organizationId(),
			asset.id(),
			command.personId(),
			command.locationId(),
			now,
			null,
			actor.userId(),
			normalizeNotes(command.notes()),
			now
		);

		AssetAssignment savedAssignment = assetAssignmentRepository.save(assignment);
		assetRepository.update(syncAssetOnAssign(asset, command.personId(), command.locationId(), now));
		recordAudit(savedAssignment, actor.userId(), AuditEventType.ASSET_ASSIGNED);

		return toView(savedAssignment);
	}

	@Transactional
	public AssetAssignmentView unassign(AuthenticatedUserPrincipal actor, UUID assetId) {
		Asset asset = findAssetForActor(actor, assetId, true);
		AssetAssignment activeAssignment = assetAssignmentRepository.findActiveByAssetIdAndOrganizationId(
			asset.id(),
			asset.organizationId()
		).orElseThrow(() -> new InvalidAssignmentRequestException("This asset does not have an active assignment."));

		Instant now = Instant.now(clock);
		AssetAssignment closedAssignment = assetAssignmentRepository.closeActiveAssignment(
			activeAssignment.id(),
			asset.organizationId(),
			now
		);
		assetRepository.update(syncAssetOnUnassign(asset, now));
		recordAudit(closedAssignment, actor.userId(), AuditEventType.ASSET_UNASSIGNED);

		return toView(closedAssignment);
	}

	@Transactional(readOnly = true)
	public List<AssetAssignmentView> list(AuthenticatedUserPrincipal actor, UUID assetId) {
		Asset asset = findAssetForActor(actor, assetId, false);
		return assetAssignmentRepository.findAllByAssetIdAndOrganizationId(
			asset.id(),
			asset.organizationId(),
			QueryLimits.DEFAULT_LIST_LIMIT
		)
			.stream()
			.map(this::toView)
			.toList();
	}

	private Asset findAssetForActor(AuthenticatedUserPrincipal actor, UUID assetId, boolean writeAccess) {
		if (actor.isSuperAdmin()) {
			return assetRepository.findById(assetId).orElseThrow(AssetNotFoundException::new);
		}

		UUID organizationId = requireActorOrganizationId(actor);
		if (writeAccess) {
			tenantAccessService.requireAssignmentWriteAccess(actor, organizationId);
		}
		else {
			tenantAccessService.requireAssignmentReadAccess(actor, organizationId);
		}

		return assetRepository.findByIdAndOrganizationId(assetId, organizationId).orElseThrow(AssetNotFoundException::new);
	}

	private void validateAssignableAsset(Asset asset) {
		if (asset.archivedAt() != null) {
			throw new InvalidAssignmentRequestException("Archived assets cannot be assigned.");
		}

		if (asset.status() == AssetStatus.RETIRED) {
			throw new InvalidAssignmentRequestException("Assets with status RETIRED cannot be assigned.");
		}
	}

	private void validateAssignee(UUID organizationId, UUID personId) {
		if (personId == null) {
			throw new InvalidAssignmentRequestException("personId is required.");
		}

		var person = personRepository.findById(personId)
			.orElseThrow(() -> new InvalidAssignmentRequestException("personId must reference an existing person."));
		if (person.organizationId() == null || !person.organizationId().equals(organizationId)) {
			throw new InvalidAssignmentRequestException("personId must belong to the same organization as the asset.");
		}
		if (!person.active()) {
			throw new InvalidAssignmentRequestException("personId must reference an active person.");
		}
	}

	private void validateLocation(UUID organizationId, UUID locationId) {
		if (locationId == null) {
			return;
		}

		var location = locationRepository.findByIdAndOrganizationId(locationId, organizationId)
			.orElseThrow(() -> new InvalidAssignmentRequestException("locationId must belong to the same organization as the asset."));
		if (!location.active()) {
			throw new InvalidAssignmentRequestException("locationId must reference an active location.");
		}
	}

	private Asset syncAssetOnAssign(Asset asset, UUID personId, UUID locationId, Instant updatedAt) {
		return new Asset(
			asset.id(),
			asset.organizationId(),
			asset.assetTag(),
			asset.serialNumber(),
			asset.hostname(),
			asset.displayName(),
			asset.description(),
			asset.categoryId(),
			asset.manufacturerId(),
			locationId != null ? locationId : asset.currentLocationId(),
			personId,
			null,
			AssetStatus.ASSIGNED,
			asset.purchaseDate(),
			asset.warrantyExpiryDate(),
			asset.archivedAt(),
			asset.createdAt(),
			updatedAt
		);
	}

	private Asset syncAssetOnUnassign(Asset asset, Instant updatedAt) {
		return new Asset(
			asset.id(),
			asset.organizationId(),
			asset.assetTag(),
			asset.serialNumber(),
			asset.hostname(),
			asset.displayName(),
			asset.description(),
			asset.categoryId(),
			asset.manufacturerId(),
			asset.currentLocationId(),
			null,
			null,
			asset.status() == AssetStatus.ASSIGNED ? AssetStatus.IN_STOCK : asset.status(),
			asset.purchaseDate(),
			asset.warrantyExpiryDate(),
			asset.archivedAt(),
			asset.createdAt(),
			updatedAt
		);
	}

	private void recordAudit(AssetAssignment assignment, UUID actorUserId, AuditEventType eventType) {
		auditLogService.recordInCurrentTransaction(new AuditLogCommand(
			assignment.organizationId(),
			actorUserId,
			eventType,
			"asset_assignment",
			assignment.id(),
			"SUCCESS",
			Map.of(
				"assetId", assignment.assetId(),
				"personId", assignment.personId()
			)
		));
	}

	private AssetAssignmentView toView(AssetAssignment assignment) {
		return new AssetAssignmentView(
			assignment.id(),
			assignment.assetId(),
			assignment.personId(),
			assignment.locationId(),
			assignment.assignedAt(),
			assignment.unassignedAt(),
			assignment.assignedBy(),
			assignment.notes(),
			assignment.createdAt()
		);
	}

	private UUID requireActorOrganizationId(AuthenticatedUserPrincipal actor) {
		if (actor.organizationId() == null) {
			throw new InvalidAssignmentRequestException("Tenant organization context is required for assignment operations.");
		}

		return actor.organizationId();
	}

	private String normalizeNotes(String notes) {
		return notes == null || notes.isBlank() ? null : notes.trim();
	}
}
