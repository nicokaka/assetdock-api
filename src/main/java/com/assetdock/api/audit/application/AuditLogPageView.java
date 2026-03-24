package com.assetdock.api.audit.application;

import java.util.List;

public record AuditLogPageView(
	List<AuditLogView> items,
	int page,
	int size,
	long totalElements,
	int totalPages
) {
}
