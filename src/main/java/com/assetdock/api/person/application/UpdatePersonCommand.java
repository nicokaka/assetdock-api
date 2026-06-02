package com.assetdock.api.person.application;

public record UpdatePersonCommand(
	String fullName,
	String email,
	String department,
	boolean active
) {
}
