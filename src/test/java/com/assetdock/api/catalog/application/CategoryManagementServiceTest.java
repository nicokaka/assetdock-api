package com.assetdock.api.catalog.application;

import com.assetdock.api.audit.application.AuditContext;
import com.assetdock.api.audit.application.AuditContextProvider;
import com.assetdock.api.audit.application.AuditLogService;
import com.assetdock.api.audit.domain.AuditLogRepository;
import com.assetdock.api.catalog.domain.Category;
import com.assetdock.api.catalog.domain.CategoryRepository;
import com.assetdock.api.security.auth.AuthenticatedUserPrincipal;
import com.assetdock.api.security.auth.TenantAccessService;
import com.assetdock.api.user.domain.UserRole;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
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
class CategoryManagementServiceTest {

	private static final Instant NOW = Instant.parse("2026-03-18T12:00:00Z");
	private static final UUID ORG_1 = UUID.fromString("11111111-1111-1111-1111-111111111111");

	@Mock
	private CategoryRepository categoryRepository;

	private CategoryManagementService categoryManagementService;

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

		categoryManagementService = new CategoryManagementService(
			categoryRepository,
			new TenantAccessService(),
			auditLogService,
			Clock.fixed(NOW, ZoneOffset.UTC)
		);
	}

	@Test
	void orgAdminShouldCreateCategoryInOwnOrganization() {
		AuthenticatedUserPrincipal actor = new AuthenticatedUserPrincipal(
			UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
			ORG_1,
			"orgadmin@assetdock.dev",
			Set.of(UserRole.ORG_ADMIN)
		);
		when(categoryRepository.existsByOrganizationIdAndName(ORG_1, "laptops")).thenReturn(false);
		when(categoryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		CategoryView result = categoryManagementService.create(
			actor,
			new CreateCategoryCommand("Laptops", "Portable computers", true)
		);

		ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
		verify(categoryRepository).save(captor.capture());
		assertThat(captor.getValue().organizationId()).isEqualTo(ORG_1);
		assertThat(result.name()).isEqualTo("Laptops");
		assertThat(result.active()).isTrue();
	}
}
