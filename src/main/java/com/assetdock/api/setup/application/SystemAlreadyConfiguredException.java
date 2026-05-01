package com.assetdock.api.setup.application;

public class SystemAlreadyConfiguredException extends RuntimeException {

	public SystemAlreadyConfiguredException() {
		super("The system has already been configured.");
	}
}
