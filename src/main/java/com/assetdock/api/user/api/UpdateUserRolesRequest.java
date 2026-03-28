package com.assetdock.api.user.api;

import com.assetdock.api.user.domain.UserRole;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

public record UpdateUserRolesRequest(@NotEmpty Set<UserRole> roles) {
}
