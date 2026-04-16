package com.assetdock.api.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

/**
 * Enforces the AssetDock password policy.
 *
 * <p>Validation rules (all must pass):
 * <ol>
 *   <li>Length: 8–72 characters</li>
 *   <li>At least one ASCII uppercase letter (A-Z)</li>
 *   <li>At least one ASCII digit (0-9)</li>
 *   <li>At least one special character from the allowed set</li>
 * </ol>
 *
 * <p>A null value is considered valid — use {@code @NotBlank} in combination to reject nulls.
 */
public class PasswordPolicyValidator implements ConstraintValidator<ValidPassword, String> {

	private static final int MIN_LENGTH = 8;
	private static final int MAX_LENGTH = 72;

	private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");
	private static final Pattern DIGIT = Pattern.compile("[0-9]");
	private static final Pattern SPECIAL = Pattern.compile("[!@#$%^&*()\\-_=+\\[\\]{}|;:'\",.<>?/`~]");

	@Override
	public boolean isValid(String password, ConstraintValidatorContext context) {
		if (password == null) {
			return true; // @NotBlank handles null
		}

		int length = password.length();
		if (length < MIN_LENGTH || length > MAX_LENGTH) {
			return false;
		}

		if (!UPPERCASE.matcher(password).find()) {
			return false;
		}

		if (!DIGIT.matcher(password).find()) {
			return false;
		}

		return SPECIAL.matcher(password).find();
	}
}
