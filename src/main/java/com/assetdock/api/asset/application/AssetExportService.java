package com.assetdock.api.asset.application;

import com.assetdock.api.asset.domain.Asset;
import com.assetdock.api.asset.domain.AssetRepository;
import com.assetdock.api.security.auth.AuthenticatedUserPrincipal;
import com.assetdock.api.security.auth.TenantAccessService;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssetExportService {

	private final AssetRepository assetRepository;
	private final TenantAccessService tenantAccessService;

	public AssetExportService(AssetRepository assetRepository, TenantAccessService tenantAccessService) {
		this.assetRepository = assetRepository;
		this.tenantAccessService = tenantAccessService;
	}

	@Transactional(readOnly = true)
	public void exportCsv(AuthenticatedUserPrincipal actor, java.io.OutputStream outputStream) throws IOException {
		UUID organizationId = requireActorOrganizationId(actor);
		tenantAccessService.requireAssetReadAccess(actor, organizationId);

		int limit = 500;
		int offset = 0;
		List<Asset> assets;

		try (OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
			 CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder().setHeader(
				 "Asset Tag", "Display Name", "Status", "Serial Number", "Hostname", "Description",
				 "Category ID", "Manufacturer ID", "Location ID", "Assigned User ID",
				 "Purchase Date", "Warranty Expiry"
			 ).build())) {

			do {
				// We reuse paginated logic for the export chunking to avoid OOM
				assets = assetRepository.findAllPaginated(organizationId, limit, offset, null, null);
				for (Asset asset : assets) {
					printer.printRecord(
						asset.assetTag(),
						asset.displayName(),
						asset.status().name(),
						asset.serialNumber(),
						asset.hostname(),
						asset.description(),
						asset.categoryId(),
						asset.manufacturerId(),
						asset.currentLocationId(),
						asset.currentAssignedUserId(),
						asset.purchaseDate(),
						asset.warrantyExpiryDate()
					);
				}
				offset += limit;
			} while (assets.size() == limit);

		}
	}

	private UUID requireActorOrganizationId(AuthenticatedUserPrincipal actor) {
		if (actor.organizationId() == null) {
			throw new IllegalStateException("Tenant organization context is required for export.");
		}
		return actor.organizationId();
	}
}
