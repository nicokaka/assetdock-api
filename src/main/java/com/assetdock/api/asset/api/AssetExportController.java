package com.assetdock.api.asset.api;

import com.assetdock.api.asset.application.AssetExportService;
import com.assetdock.api.security.auth.AuthenticatedUserPrincipal;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/assets/export")
public class AssetExportController {

	private final AssetExportService assetExportService;

	public AssetExportController(AssetExportService assetExportService) {
		this.assetExportService = assetExportService;
	}

	@GetMapping(produces = "text/csv")
	public void exportAssets(
		@AuthenticationPrincipal AuthenticatedUserPrincipal principal,
		HttpServletResponse response
	) throws IOException {
		response.setContentType("text/csv");
		response.setHeader("Content-Disposition", "attachment; filename=\"assets_export.csv\"");

		assetExportService.exportCsv(principal, response.getOutputStream());
	}
}
