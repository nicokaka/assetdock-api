package com.assetdock.api.user.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface UserRepository {

	Optional<User> findByEmail(String normalizedEmail);

	Optional<User> findById(UUID userId);

	List<User> findAll(int limit);

	List<User> findAllPaginated(UUID organizationId, int limit, int offset, String search);

	long countForOrganization(UUID organizationId, String search);

	boolean existsByEmail(String normalizedEmail);

	User save(User user);

	User updateStatus(UUID userId, UserStatus status, Instant updatedAt);

	User updateRoles(UUID userId, Set<UserRole> roles, Instant updatedAt);

	User incrementFailedLoginAttempts(UUID userId, Instant updatedAt);

	User resetFailedLoginAttempts(UUID userId, Instant updatedAt);

	long countActiveUsersByOrganizationIdAndRole(UUID organizationId, UserRole role);

	long countActiveUsersByRole(UserRole role);

	User updateProfile(UUID userId, String fullName, String normalizedEmail, Instant updatedAt);

	void updateLastLoginAt(UUID userId, Instant lastLoginAt);

	int countTotalUsersForOrganization(UUID organizationId);

	int countActiveUsersForOrganization(UUID organizationId);
}
