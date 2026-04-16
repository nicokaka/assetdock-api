package com.assetdock.api.asset.application;

import java.util.List;

public record AssetPageView(
	List<AssetView> items,
	int page,
	int size,
	long totalItems,
	int totalPages
) {
}
