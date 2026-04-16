package com.assetdock.api.user.application;

import java.util.List;

public record UserPageView(
	List<UserView> items,
	int page,
	int size,
	long totalItems,
	int totalPages
) {
}
