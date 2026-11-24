package com.assetdock.api.asset.application;

import com.assetdock.api.asset.domain.Asset;
import com.assetdock.api.asset.domain.AssetRepository;
import com.assetdock.api.asset.domain.AssetStatus;
import com.assetdock.api.security.auth.AuthenticatedUserPrincipal;
import com.assetdock.api.security.auth.TenantAccessService;
import com.assetdock.api.user.domain.UserRole;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssetExportServiceTest {

	private static final UUID ORG_1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID ASSET_1 = UUID.fromString("22222222-2222-2222-2222-222222222222");

	@Mock
	private AssetRepository assetRepository;

	private AssetExportService assetExportService;

	@BeforeEach
	void setUp() {
		assetExportService = new AssetExportService(assetRepository, new TenantAccessService());
	}

	@Test
	void neutralizesFormulaInjectionPrefixesInExportedCsvCells() throws Exception {
		AuthenticatedUserPrincipal actor = new AuthenticatedUserPrincipal(
			UUID.randomUUID(),
			ORG_1,
			"admin@assetdock.dev",
			Set.of(UserRole.ORG_ADMIN)
		);
		Instant now = Instant.parse("2026-04-01T10:00:00Z");
		Asset asset = new Asset(
			ASSET_1,
			ORG_1,
			"=CMD('calc')",
			"+12345",
			"-host",
			"@SUM(A1:A2)",
			"\tformula",
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
		when(assetRepository.findAllPaginated(ORG_1, 500, 0, null, null, null, null))
			.thenReturn(List.of(asset));

		ByteArrayOutputStream output = new ByteArrayOutputStream();
		assetExportService.exportCsv(actor, output);

		String csv = output.toString(StandardCharsets.UTF_8);
		assertThat(csv)
			.contains("'=CMD('calc')")
			.contains("'+12345")
			.contains("'-host")
			.contains("'@SUM(A1:A2)")
			.contains("'\tformula");
	}

	@Test
	void rejectsGlobalActorWithoutOrganizationContextAsInvalidAssetRequest() {
		AuthenticatedUserPrincipal superAdmin = new AuthenticatedUserPrincipal(
			UUID.randomUUID(),
			null,
			"super@assetdock.dev",
			Set.of(UserRole.SUPER_ADMIN)
		);

		assertThatThrownBy(() -> assetExportService.validateAccess(superAdmin))
			.isInstanceOf(InvalidAssetRequestException.class);
	}
}
