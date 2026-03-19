package com.assetdock.api.organization.application;

public class OrganizationNotFoundException extends RuntimeException {

	public OrganizationNotFoundException() {
		super("Organization not found.");
	}
}
