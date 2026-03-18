package com.assetdock.api.user.domain;

public enum UserRole {
	SUPER_ADMIN,
	ORG_ADMIN,
	ASSET_MANAGER,
	AUDITOR,
	VIEWER;

	public String authority() {
		return "ROLE_" + name();
	}
}
