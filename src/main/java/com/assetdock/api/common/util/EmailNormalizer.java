package com.assetdock.api.common.util;

import java.util.Locale;

public final class EmailNormalizer {

	private EmailNormalizer() {
		// Prevent instantiation
	}

	public static String normalize(String email) {
		if (email == null) {
			return null;
		}
		return email.trim().toLowerCase(Locale.ROOT);
	}
}
