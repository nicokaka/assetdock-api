package com.assetdock.api.catalog.application;

import com.assetdock.api.audit.application.AuditContext;
import com.assetdock.api.audit.application.AuditContextProvider;
import com.assetdock.api.audit.application.AuditLogService;
import com.assetdock.api.audit.domain.AuditLogRepository;
import com.assetdock.api.catalog.domain.Manufacturer;
import com.assetdock.api.catalog.domain.ManufacturerRepository;
import com.assetdock.api.security.auth.AuthenticatedUserPrincipal;
import com.assetdock.api.security.auth.TenantAccessService;
import com.assetdock.api.user.domain.UserRole;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManufacturerManagementServiceTest {

	private static final Instant NOW = Instant.parse("2026-03-18T12:00:00Z");
	private static final UUID ORG_1 = UUID.fromString("11111111-1111-1111-1111-111111111111");

	@Mock
	private ManufacturerRepository manufacturerRepository;

	private ManufacturerManagementService manufacturerManagementService;

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

		manufacturerManagementService = new ManufacturerManagementService(
			manufacturerRepository,
			new TenantAccessService(),
			auditLogService,
			Clock.fixed(NOW, ZoneOffset.UTC)
		);
	}

	@Test
	void assetManagerShouldCreateManufacturerInOwnOrganization() {
		AuthenticatedUserPrincipal actor = new AuthenticatedUserPrincipal(
			UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
			ORG_1,
			"manager@assetdock.dev",
			Set.of(UserRole.ASSET_MANAGER)
		);
		when(manufacturerRepository.existsByOrganizationIdAndName(ORG_1, "lenovo")).thenReturn(false);
		when(manufacturerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		ManufacturerView result = manufacturerManagementService.create(
			actor,
			new CreateManufacturerCommand("Lenovo", "PC vendor", "https://lenovo.example", true)
		);

		ArgumentCaptor<Manufacturer> captor = ArgumentCaptor.forClass(Manufacturer.class);
		verify(manufacturerRepository).save(captor.capture());
		assertThat(captor.getValue().organizationId()).isEqualTo(ORG_1);
		assertThat(result.name()).isEqualTo("Lenovo");
		assertThat(result.website()).isEqualTo("https://lenovo.example");
	}
}
