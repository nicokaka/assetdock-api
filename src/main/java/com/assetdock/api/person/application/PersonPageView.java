package com.assetdock.api.person.application;

import java.util.List;

public record PersonPageView(
	List<PersonView> items,
	int page,
	int size,
	long totalItems,
	int totalPages
) {
}
