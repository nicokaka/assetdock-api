package com.assetdock.api.importer.api;

import com.assetdock.api.importer.application.AssetCsvImportService;
import com.assetdock.api.importer.application.AssetImportJobView;
import com.assetdock.api.security.auth.AuthenticatedUserPrincipal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/web/imports/assets")
public class AssetCsvImportController {

	private final AssetCsvImportService assetCsvImportService;

	public AssetCsvImportController(AssetCsvImportService assetCsvImportService) {
		this.assetCsvImportService = assetCsvImportService;
	}

	@PostMapping(path = "/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	AssetImportJobView importCsv(
		@AuthenticationPrincipal AuthenticatedUserPrincipal principal,
		@RequestPart("file") MultipartFile file
	) {
		return assetCsvImportService.importCsv(principal, file);
	}

	@GetMapping("/{jobId}")
	AssetImportJobView getJob(
		@PathVariable UUID jobId,
		@AuthenticationPrincipal AuthenticatedUserPrincipal principal
	) {
		return assetCsvImportService.getJob(principal, jobId);
	}
}
