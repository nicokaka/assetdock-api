package com.assetdock.api.search.application;

import java.util.List;

public record GlobalSearchResult(
	List<SearchResultItem> assets,
	List<SearchResultItem> users
) {
	public record SearchResultItem(
		String id,
		String title,
		String subtitle,
		String urlPath
	) {}
}
