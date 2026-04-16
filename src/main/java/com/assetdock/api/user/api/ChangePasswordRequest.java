package com.assetdock.api.user.api;

import com.assetdock.api.common.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
	@NotBlank String currentPassword,
	@NotBlank @ValidPassword String newPassword
) {
}
