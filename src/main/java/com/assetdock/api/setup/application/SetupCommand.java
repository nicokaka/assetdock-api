package com.assetdock.api.setup.application;


public record SetupCommand(
	String organizationName,
	String adminFullName,
	String adminEmail,
	String adminPassword
) {
}
