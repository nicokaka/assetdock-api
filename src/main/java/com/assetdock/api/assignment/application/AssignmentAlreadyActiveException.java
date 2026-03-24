package com.assetdock.api.assignment.application;

public class AssignmentAlreadyActiveException extends RuntimeException {

	public AssignmentAlreadyActiveException() {
		super("This asset already has an active assignment.");
	}
}
