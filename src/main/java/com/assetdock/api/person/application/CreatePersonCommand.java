package com.assetdock.api.person.application;

public record CreatePersonCommand(
	String fullName,
	String email,
	String department,
	boolean active
) {
}
