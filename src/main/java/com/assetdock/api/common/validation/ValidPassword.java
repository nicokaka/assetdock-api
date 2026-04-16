package com.assetdock.api.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that a password satisfies the AssetDock password policy:
 * <ul>
 *   <li>Minimum 8 characters</li>
 *   <li>At least one uppercase letter</li>
 *   <li>At least one digit</li>
 *   <li>At least one special character from: {@code !@#$%^&*()-_=+[]{}|;:'",.<>?/`~}</li>
 * </ul>
 */
@Documented
@Constraint(validatedBy = PasswordPolicyValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPassword {

	String message() default "Password must be 8–72 characters and contain at least one uppercase letter, one digit, and one special character.";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
