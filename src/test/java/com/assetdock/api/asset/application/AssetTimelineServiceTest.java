package com.assetdock.api.asset.application;

import com.assetdock.api.asset.domain.Asset;
import com.assetdock.api.asset.domain.AssetRepository;
import com.assetdock.api.asset.domain.AssetStatus;
import com.assetdock.api.audit.domain.AuditEventType;
import com.assetdock.api.audit.domain.AuditLogEntry;
import com.assetdock.api.audit.domain.AuditLogRepository;
import com.assetdock.api.security.auth.AuthenticatedUserPrincipal;
import com.assetdock.api.security.auth.TenantAccessService;
import com.assetdock.api.user.domain.UserRole;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssetTimelineServiceTest {

	private static final UUID ORG_1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID ASSET_1 = UUID.fromString("22222222-2222-2222-2222-222222222222");

	@Mock
	private AuditLogRepository auditLogRepository;

	@Mock
	private AssetRepository assetRepository;

	private AssetTimelineService assetTimelineService;

	@BeforeEach
	void setUp() {
		assetTimelineService = new AssetTimelineService(auditLogRepository, assetRepository, new TenantAccessService());
	}

	@Test
	void superAdminReadsTimelineAcrossOrganizations() {
		AuthenticatedUserPrincipal superAdmin = new AuthenticatedUserPrincipal(
			UUID.randomUUID(),
			null,
			"super@assetdock.dev",
			Set.of(UserRole.SUPER_ADMIN)
		);
		Instant now = Instant.parse("2026-04-01T10:00:00Z");
		Asset asset = new Asset(
			ASSET_1,
			ORG_1,
			"AST-001",
			null,
			null,
			"MacBook",
			null,
			null,
			null,
			null,
			null,
			null,
			AssetStatus.IN_STOCK,
			null,
			null,
			null,
			now,
			now
		);
		when(assetRepository.findById(ASSET_1)).thenReturn(Optional.of(asset));
		when(auditLogRepository.findByResourceId(ORG_1, ASSET_1)).thenReturn(List.of(
			new AuditLogEntry(
				UUID.randomUUID(),
				ORG_1,
				UUID.randomUUID(),
				AuditEventType.ASSET_CREATED,
				"asset",
				ASSET_1,
				"SUCCESS",
				"127.0.0.1",
				"JUnit",
				"req-1",
				Map.of("assetTag", "AST-001"),
				now
			)
		));

		List<TimelineEventView> result = assetTimelineService.getAssetTimeline(superAdmin, ASSET_1);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).eventType()).isEqualTo(AuditEventType.ASSET_CREATED);
	}

	@Test
	void tenantPrincipalWithoutReadRolesIsDenied() {
		AuthenticatedUserPrincipal emptyRoleActor = new AuthenticatedUserPrincipal(
			UUID.randomUUID(),
			ORG_1,
			"norole@assetdock.dev",
			Set.of()
		);

		assertThatThrownBy(() -> assetTimelineService.getAssetTimeline(emptyRoleActor, ASSET_1))
			.isInstanceOf(AccessDeniedException.class);
	}
}
