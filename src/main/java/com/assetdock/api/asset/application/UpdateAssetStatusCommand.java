package com.assetdock.api.asset.application;

import com.assetdock.api.asset.domain.AssetStatus;

public record UpdateAssetStatusCommand(AssetStatus status) {
}
