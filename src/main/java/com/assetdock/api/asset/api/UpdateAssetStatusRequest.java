package com.assetdock.api.asset.api;

import com.assetdock.api.asset.domain.AssetStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateAssetStatusRequest(
	@NotNull(message = "status is required")
	AssetStatus status
) {
}
