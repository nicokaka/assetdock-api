package com.assetdock.api.setup.application;

import com.assetdock.api.organization.domain.Organization;
import com.assetdock.api.user.domain.User;

public record SetupResult(Organization organization, User admin) {
}
