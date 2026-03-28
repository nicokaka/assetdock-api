package com.assetdock.api.catalog.application;

import com.assetdock.api.audit.application.AuditContext;
import com.assetdock.api.audit.application.AuditContextProvider;
import com.assetdock.api.audit.application.AuditLogService;
import com.assetdock.api.audit.domain.AuditLogRepository;
import com.assetdock.api.catalog.domain.Category;
import com.assetdock.api.catalog.domain.CategoryRepository;
import com.assetdock.api.catalog.domain.Location;
import com.assetdock.api.catalog.domain.LocationRepository;
import com.assetdock.api.security.auth.AuthenticatedUserPrincipal;
import com.assetdock.api.security.auth.TenantAccessService;
import com.assetdock.api.user.domain.UserRole;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
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
class CatalogReadAccessTest {

	private static final UUID ORG_1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID ORG_2 = UUID.fromString("22222222-2222-2222-2222-222222222222");
	private static final Instant NOW = Instant.parse("2026-03-18T12:00:00Z");

	@Mock
	private CategoryRepository categoryRepository;

	@Mock
	private LocationRepository locationRepository;

	private CategoryManagementService categoryManagementService;
	private LocationManagementService locationManagementService;

	@BeforeEach
	void setUp() {
		AuditLogService auditLogService = new AuditLogService(
			org.mockito.Mockito.mock(AuditLogRepository.class),
			new AuditContextProvider() {
				@Override
				public AuditContext current() {
					return new AuditContext("127.0.0.1", "JUnit", "test-request-id");
				}
			},
			Clock.fixed(NOW, ZoneOffset.UTC)
		);

		TenantAccessService tenantAccessService = new TenantAccessService();
		categoryManagementService = new CategoryManagementService(
			categoryRepository,
			tenantAccessService,
			auditLogService,
			Clock.fixed(NOW, ZoneOffset.UTC)
		);
		locationManagementService = new LocationManagementService(
			locationRepository,
			tenantAccessService,
			auditLogService,
			Clock.fixed(NOW, ZoneOffset.UTC)
		);
	}

	@Test
	void auditorShouldListCatalogsFromOwnOrganization() {
		AuthenticatedUserPrincipal actor = new AuthenticatedUserPrincipal(
			UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
			ORG_1,
			"auditor@assetdock.dev",
			Set.of(UserRole.AUDITOR)
		);
		when(categoryRepository.findAllByOrganizationId(ORG_1, 100)).thenReturn(List.of(
			new Category(UUID.randomUUID(), ORG_1, "Laptops", "Portable computers", true, NOW, NOW)
		));

		List<CategoryView> result = categoryManagementService.list(actor);

		assertThat(result).hasSize(1);
		assertThat(result.getFirst().name()).isEqualTo("Laptops");
	}

	@Test
	void viewerShouldListCatalogsFromOwnOrganization() {
		AuthenticatedUserPrincipal actor = new AuthenticatedUserPrincipal(
			UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
			ORG_1,
			"viewer@assetdock.dev",
			Set.of(UserRole.VIEWER)
		);
		when(locationRepository.findAllByOrganizationId(ORG_1, 100)).thenReturn(List.of(
			new Location(UUID.randomUUID(), ORG_1, "Warehouse", "Main storage", true, NOW, NOW)
		));

		List<LocationView> result = locationManagementService.list(actor);

		assertThat(result).hasSize(1);
		assertThat(result.getFirst().name()).isEqualTo("Warehouse");
	}

	@Test
	void crossTenantCatalogAccessShouldBeDenied() {
		AuthenticatedUserPrincipal actor = new AuthenticatedUserPrincipal(
			UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"),
			ORG_1,
			"manager@assetdock.dev",
			Set.of(UserRole.ASSET_MANAGER)
		);

		assertThatThrownBy(() -> new TenantAccessService().requireCatalogWriteAccess(actor, ORG_2))
			.isInstanceOf(AccessDeniedException.class);
	}
}
